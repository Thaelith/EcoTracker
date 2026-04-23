package com.ecotracker.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotracker.data.repository.UserRepository
import com.ecotracker.utils.Badge
import com.ecotracker.utils.GamificationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementsUiState(
    val badges: List<Badge> = emptyList()
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    repository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = repository.getUserStats()
        .map { stats ->
            AchievementsUiState(
                badges = GamificationEngine.getBadges(
                    scanCount = stats.scanCount,
                    hasVerifiedProduct = stats.verifiedBadgeUnlocked,
                    hasLowCarbonProduct = stats.lowCarbonBadgeUnlocked
                )
            )
        }
        .catch { emit(AchievementsUiState()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AchievementsUiState()
        )
}
