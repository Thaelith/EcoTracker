package com.ecotracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotracker.R
import com.ecotracker.data.repository.EcoTrackerRepository
import com.ecotracker.utils.Badge
import com.ecotracker.utils.GamificationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "—",
    val username: String = "—",
    val rankNameResId: Int = R.string.rank_seedling,
    val rankProgress: Int = 0,
    val scanCount: Int = 0,
    val unlockedBadgesPreview: List<Badge> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: EcoTrackerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        observeGamification()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val profile = repository.getCurrentUserProfile()
            _uiState.update { current ->
                current.copy(
                    email = profile.email,
                    username = profile.username
                )
            }
        }
    }

    private fun observeGamification() {
        viewModelScope.launch {
            repository.getAllProducts()
                .catch { emit(emptyList()) }
                .collect { products ->
                    val scanCount = products.size
                    val rank = GamificationEngine.calculateRank(scanCount)
                    val badges = GamificationEngine.getBadges(products)

                    _uiState.update { current ->
                        current.copy(
                            rankNameResId = rank.nameResId,
                            rankProgress = rank.percentage,
                            scanCount = scanCount,
                            unlockedBadgesPreview = badges.filter { it.isUnlocked }.take(5)
                        )
                    }
                }
        }
    }
}
