package com.ecotracker.data.repository

import com.ecotracker.data.local.EcoTrackerDatabase
import com.ecotracker.data.local.LocalUserStats
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.model.LeaderboardUser
import com.ecotracker.data.model.UserProfile
import com.ecotracker.utils.AppConfig
import com.ecotracker.utils.Logger
import com.ecotracker.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sharedPreferences: android.content.SharedPreferences,
    private val dao: ScannedProductDao
) {
    companion object {
        private const val TAG = "UserRepo"
        private const val PROFILE_PHOTO_KEY_PREFIX = "profile_photo_uri_"
        private const val GUEST_USER_ID = "__guest__"
    }

    // ── User Profile ──────────────────────────────────────────────────────────

    suspend fun getCurrentUserProfile(): UserProfile {
        val currentUser = auth.currentUser ?: return UserProfile(
            email = "Not signed in",
            username = "-",
            photoUri = null
        )

        val username = try {
            val document = firestore.collection("users").document(currentUser.uid).get().await()
            sanitizeUsername(document.getString("username").orEmpty()).ifBlank { "-" }
        } catch (e: Exception) {
            Logger.debug(TAG, "Failed to load current user profile: ${e.javaClass.simpleName}")
            "-"
        }

        return UserProfile(
            email = currentUser.email ?: "-",
            username = username,
            photoUri = getProfilePhotoUri()
        )
    }

    fun getProfilePhotoUri(): String? {
        val userKey = auth.currentUser?.uid ?: "guest"
        return sharedPreferences.getString("$PROFILE_PHOTO_KEY_PREFIX$userKey", null)
    }

    fun saveProfilePhotoUri(photoUri: String?) {
        val userKey = auth.currentUser?.uid ?: "guest"
        sharedPreferences.edit()
            .putString("$PROFILE_PHOTO_KEY_PREFIX$userKey", photoUri)
            .apply()
    }

    // ── User Stats ────────────────────────────────────────────────────────────

    fun getUserStats(): Flow<LocalUserStats> = dao.observeUserStats(currentUserKey())

    fun getTotalCarbonSince(startTime: Long): Flow<Double?> =
        dao.getTotalCarbonSince(currentUserKey(), startTime)

    fun getTotalScannedCount(): Flow<Int> = dao.getTotalScannedCount(currentUserKey())

    // ── Stats Reconciliation ──────────────────────────────────────────────────

    suspend fun reconcileCurrentUserStats() {
        val currentUser = auth.currentUser ?: return
        try {
            adoptLegacyHistoryIfNeeded(currentUser.uid)
            syncCurrentUserStats(
                userRef = firestore.collection("users").document(currentUser.uid),
                userId = currentUser.uid
            )
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to reconcile current user stats", e)
        }
    }

    suspend fun syncCurrentUserStats(
        userRef: com.google.firebase.firestore.DocumentReference,
        userId: String
    ) {
        val localStats = dao.getUserStatsValue(userId)
        val stats = mutableMapOf<String, Any>(
            "scanCount" to localStats.scanCount,
            "co2e" to localStats.totalCarbon,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val existingUser = userRef.get().await()
        if (!existingUser.exists()) {
            stats["createdAt"] = System.currentTimeMillis()
        }

        userRef.set(stats, SetOptions.merge()).await()
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────

    fun getLeaderboardUsers(): Flow<Resource<List<LeaderboardUser>>> {
        val remoteLeaderboard = observeRemoteLeaderboard()
        val currentUserId = auth.currentUser?.uid ?: return remoteLeaderboard

        return combine(
            remoteLeaderboard,
            dao.observeUserStats(currentUserId)
        ) { leaderboardResource, localStats ->
            when (leaderboardResource) {
                is Resource.Success -> {
                    Resource.Success(
                        overlayCurrentUserLeaderboardStats(
                            users = leaderboardResource.data,
                            currentUserId = currentUserId,
                            localScanCount = localStats.scanCount,
                            localCarbon = localStats.totalCarbon
                        )
                    )
                }

                is Resource.Error -> leaderboardResource
                is Resource.Loading -> leaderboardResource
                is Resource.NeedsInput -> leaderboardResource
            }
        }
    }

    // ── Private Methods ───────────────────────────────────────────────────────

    private fun observeRemoteLeaderboard(): Flow<Resource<List<LeaderboardUser>>> =
        callbackFlow {
            val listener = firestore.collection("users")
                .orderBy("scanCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(AppConfig.LEADERBOARD_MAX_SIZE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val message = when (error.code) {
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                "Permission denied. Please ensure you are logged in."

                            com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                                "Leaderboard is being initialized. Please try again in 1-2 minutes."

                            else -> error.message ?: "Failed to load leaderboard"
                        }
                        trySend(Resource.Error(message))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val users = snapshot.documents.mapIndexed { index, doc ->
                            val rawUsername = doc.getString("username") ?: "Anonymous"
                            LeaderboardUser(
                                uid = doc.id,
                                rank = index + 1,
                                username = sanitizeUsername(rawUsername),
                                scanCount = doc.getLong("scanCount")?.toInt() ?: 0,
                                co2e = doc.getDouble("co2e") ?: 0.0
                            )
                        }
                        trySend(Resource.Success(users))
                    }
                }
            awaitClose { listener.remove() }
        }

    private fun overlayCurrentUserLeaderboardStats(
        users: List<LeaderboardUser>,
        currentUserId: String,
        localScanCount: Int,
        localCarbon: Double
    ): List<LeaderboardUser> {
        val adjustedUsers = users.map { user ->
            if (user.uid == currentUserId) {
                user.copy(
                    scanCount = localScanCount,
                    co2e = localCarbon
                )
            } else {
                user
            }
        }

        return adjustedUsers
            .sortedWith(
                compareByDescending<LeaderboardUser> { it.scanCount }
                    .thenByDescending { it.co2e }
                    .thenBy { it.username.lowercase() }
            )
            .mapIndexed { index, user -> user.copy(rank = index + 1) }
    }

    private suspend fun adoptLegacyHistoryIfNeeded(currentUserId: String) {
        val currentLocalCount = dao.getTotalScannedCountValue(currentUserId)
        val legacyLocalCount = dao.getTotalScannedCountValue(EcoTrackerDatabase.LEGACY_USER_ID)

        if (legacyLocalCount == 0) {
            return
        }

        if (currentLocalCount > 0) {
            return
        }

        val userRef = firestore.collection("users").document(currentUserId)
        val userSnapshot = userRef.get().await()
        val remoteScanCount = userRef.collection("scans").get().await().size()
        val persistedScanCount = userSnapshot.getLong("scanCount")?.toInt() ?: 0
        val hasHistoricalSignal = remoteScanCount > 0 || persistedScanCount > 0
        if (!hasHistoricalSignal) {
            return
        }

        val legacyProducts = dao.getAllProducts(EcoTrackerDatabase.LEGACY_USER_ID).first()
        val movedRows = dao.reassignScanHistoryUser(
            fromUserId = EcoTrackerDatabase.LEGACY_USER_ID,
            toUserId = currentUserId
        )

        if (movedRows > 0) {
            backfillRemoteScans(userRef, legacyProducts)
            Logger.debug(TAG, "Adopted $movedRows legacy local history rows for $currentUserId")
        }
    }

    private suspend fun backfillRemoteScans(
        userRef: com.google.firebase.firestore.DocumentReference,
        products: List<ScannedProduct>
    ) {
        products.forEach { product ->
            val remoteId = "${product.barcode}_${product.timestamp}"
            val scanData = hashMapOf(
                "barcode" to product.barcode,
                "productName" to product.productName,
                "carbonFootprint" to product.carbonFootprint,
                "status" to product.status.name,
                "timestamp" to product.timestamp
            )
            userRef.collection("scans").document(remoteId).set(scanData, SetOptions.merge()).await()
        }
    }

    private fun currentUserKey(): String {
        return auth.currentUser?.uid ?: GUEST_USER_ID
    }

    private fun sanitizeUsername(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_ \\-]"), "")
            .take(AppConfig.USERNAME_MAX_LENGTH)
            .ifBlank { "Anonymous" }
    }
}
