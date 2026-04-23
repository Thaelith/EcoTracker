package com.ecotracker.data.repository

import android.content.SharedPreferences
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dao: ScannedProductDao
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        auth = mockk()
        firestore = mockk()
        sharedPreferences = mockk(relaxed = true)
        dao = mockk(relaxed = true)

        every { auth.currentUser } returns null
        every { firestore.collection(any()) } throws RuntimeException("Firestore disabled in unit test")

        repository = UserRepository(
            auth,
            firestore,
            sharedPreferences,
            dao
        )
    }

    @Test
    fun `getCurrentUserProfile returns guest when not authenticated`() = runTest {
        every { auth.currentUser } returns null

        val profile = repository.getCurrentUserProfile()

        assertEquals("Not signed in", profile.email)
        assertEquals("-", profile.username)
        assertNull(profile.photoUri)
    }

    @Test
    fun `getProfilePhotoUri returns null when no user`() {
        every { auth.currentUser } returns null
        every { sharedPreferences.getString("profile_photo_uri_guest", null) } returns null

        val result = repository.getProfilePhotoUri()

        assertNull(result)
    }

    @Test
    fun `getProfilePhotoUri returns stored uri for current user`() {
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns "user-123"
        every { sharedPreferences.getString("profile_photo_uri_user-123", null) } returns "file:///path/to/photo.jpg"

        val result = repository.getProfilePhotoUri()

        assertEquals("file:///path/to/photo.jpg", result)
    }

    @Test
    fun `saveProfilePhotoUri stores uri for current user`() {
        val user = mockk<FirebaseUser>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { auth.currentUser } returns user
        every { user.uid } returns "user-456"
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor

        repository.saveProfilePhotoUri("file:///new/photo.jpg")

        verify { editor.putString("profile_photo_uri_user-456", "file:///new/photo.jpg") }
        verify { editor.apply() }
    }

    @Test
    fun `saveProfilePhotoUri stores for guest when no user`() {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { auth.currentUser } returns null
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor

        repository.saveProfilePhotoUri("file:///guest/photo.jpg")

        verify { editor.putString("profile_photo_uri_guest", "file:///guest/photo.jpg") }
        verify { editor.apply() }
    }
}
