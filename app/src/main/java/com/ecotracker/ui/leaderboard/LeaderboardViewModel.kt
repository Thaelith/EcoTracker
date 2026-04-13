package com.ecotracker.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotracker.data.model.LeaderboardUser
import com.ecotracker.data.repository.EcoTrackerRepository
import com.ecotracker.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: EcoTrackerRepository
) : ViewModel() {

    private val _leaderboardState = MutableStateFlow<Resource<List<LeaderboardUser>>>(Resource.Success(emptyList()))
    val leaderboardState: StateFlow<Resource<List<LeaderboardUser>>> = _leaderboardState

    private var leaderboardJob: kotlinx.coroutines.Job? = null

    init {
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            _leaderboardState.value = Resource.Success(emptyList()) 
            repository.getLeaderboardUsers().collect { resource ->
                _leaderboardState.value = resource
            }
        }
    }
}
