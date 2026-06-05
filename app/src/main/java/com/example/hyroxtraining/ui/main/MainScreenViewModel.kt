package com.example.hyroxtraining.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hyroxtraining.data.DataRepository
import com.example.hyroxtraining.data.MeasureType
import com.example.hyroxtraining.data.MeasureValue
import com.example.hyroxtraining.data.Workout
import com.example.hyroxtraining.data.WorkoutResult
import com.example.hyroxtraining.data.ScheduleItem
import com.example.hyroxtraining.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.runtime.mutableStateMapOf

data class MainUiState(
    val workouts: List<Workout> = emptyList(),
    val results: List<WorkoutResult> = emptyList(),
    val scheduleList: List<ScheduleItem> = emptyList(),
    val userProfile: UserProfile? = null,
    val hasLoadedProfile: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true
)


class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    init {
        triggerSync()
    }

    // Persistent draft state maps across screen transitions
    val workoutDraftInputs = mutableStateMapOf<String, Map<MeasureType, String>>()
    val workoutDraftCheckmarks = mutableStateMapOf<String, Map<MeasureType, Boolean>>()
    val workoutDraftWeightUnits = mutableStateMapOf<String, Map<MeasureType, String>>()
    val workoutDraftNotes = mutableStateMapOf<String, String>()

    private val _isSyncing = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState> = combine(
        repository.getWorkouts(),
        repository.getResults(),
        repository.getSchedule(),
        repository.getUserProfile(),
        _isSyncing
    ) { workouts, results, schedule, profile, syncing ->
        MainUiState(
            workouts = workouts,
            results = results,
            scheduleList = schedule,
            userProfile = profile,
            hasLoadedProfile = true,
            isSyncing = syncing,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(isLoading = true)
    )

    fun createCustomWorkout(name: String, description: String, measures: List<MeasureType>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val workout = Workout(
                id = "custom_" + UUID.randomUUID().toString().take(8),
                name = name,
                description = description,
                isCustom = true,
                measures = measures
            )
            repository.addWorkout(workout)
        }
    }

    fun updateCustomWorkout(id: String, name: String, description: String, measures: List<MeasureType>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getWorkoutById(id) ?: return@launch
            val updated = existing.copy(
                name = name,
                description = description,
                measures = measures
            )
            repository.addWorkout(updated)
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            // Clear scheduled items references in SQLite & cloud
            val currentSchedule = uiState.value.scheduleList
            currentSchedule.forEach { item ->
                if (item.plannedWorkoutIds.contains(workoutId)) {
                    val remainingIds = item.plannedWorkoutIds.filter { it != workoutId }
                    repository.updateScheduleItem(
                        item.dayIndex,
                        remainingIds,
                        isCompleted = if (remainingIds.isEmpty()) false else item.isCompleted
                    )
                }
            }
        }
    }

    fun logWorkoutResult(workoutId: String, measureValues: List<MeasureValue>, notes: String = "") {
        viewModelScope.launch {
            val workoutName = uiState.value.workouts.find { it.id == workoutId }?.name ?: ""
            val result = WorkoutResult(
                id = "res_" + UUID.randomUUID().toString().take(8),
                workoutId = workoutId,
                timestamp = System.currentTimeMillis(),
                notes = notes,
                values = measureValues,
                workoutName = workoutName
            )
            repository.addResult(result)

            // Auto-complete schedule item if logged today's workout in SQLite & cloud
            val calendar = java.util.Calendar.getInstance()
            val calendarDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val todayIndex = when (calendarDay) {
                java.util.Calendar.MONDAY -> 1
                java.util.Calendar.TUESDAY -> 2
                java.util.Calendar.WEDNESDAY -> 3
                java.util.Calendar.THURSDAY -> 4
                java.util.Calendar.FRIDAY -> 5
                java.util.Calendar.SATURDAY -> 6
                java.util.Calendar.SUNDAY -> 7
                else -> 1
            }

            val currentSchedule = uiState.value.scheduleList
            currentSchedule.forEach { item ->
                if (item.dayIndex == todayIndex && item.plannedWorkoutIds.contains(workoutId) && !item.isCompleted) {
                    repository.updateScheduleItem(item.dayIndex, item.plannedWorkoutIds, isCompleted = true)
                }
            }
        }
    }

    fun deleteResult(resultId: String) {
        viewModelScope.launch {
            repository.deleteResult(resultId)
        }
    }

    fun updateWorkoutResult(resultId: String, workoutId: String, timestamp: Long, measureValues: List<MeasureValue>, notes: String = "") {
        viewModelScope.launch {
            val workoutName = uiState.value.workouts.find { it.id == workoutId }?.name ?: ""
            val updatedValues = measureValues.map { it.copy(resultId = resultId) }
            val result = WorkoutResult(
                id = resultId,
                workoutId = workoutId,
                timestamp = timestamp,
                notes = notes,
                values = updatedValues,
                workoutName = workoutName
            )
            repository.addResult(result)
        }
    }

    fun scheduleWorkouts(dayIndex: Int, workoutIds: List<String>) {
        viewModelScope.launch {
            repository.updateScheduleItem(dayIndex, workoutIds, isCompleted = false)
        }
    }

    fun toggleScheduleItemCompletion(dayIndex: Int) {
        viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            val calendarDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val todayIndex = when (calendarDay) {
                java.util.Calendar.MONDAY -> 1
                java.util.Calendar.TUESDAY -> 2
                java.util.Calendar.WEDNESDAY -> 3
                java.util.Calendar.THURSDAY -> 4
                java.util.Calendar.FRIDAY -> 5
                java.util.Calendar.SATURDAY -> 6
                java.util.Calendar.SUNDAY -> 7
                else -> 1
            }
            if (dayIndex > todayIndex) return@launch // Safety guard: cannot complete future days

            val currentItem = uiState.value.scheduleList.find { it.dayIndex == dayIndex } ?: return@launch
            repository.updateScheduleItem(dayIndex, currentItem.plannedWorkoutIds, !currentItem.isCompleted)
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.triggerSync()
            _isSyncing.value = false
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.triggerSync()
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            repository.clearLocalData()
            workoutDraftInputs.clear()
            workoutDraftCheckmarks.clear()
            workoutDraftWeightUnits.clear()
            workoutDraftNotes.clear()
        }
    }

    fun deleteUserDataLocally() {
        viewModelScope.launch {
            repository.deleteUserDataLocally()
        }
    }

    fun saveUserProfile(
        firstName: String,
        lastName: String,
        dob: String,
        country: String,
        state: String,
        city: String,
        pincode: String
    ) {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val currentProfile = uiState.value.userProfile
            val profile = UserProfile(
                userId = currentUid,
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dob,
                country = country,
                state = state,
                city = city,
                pincode = pincode,
                status = currentProfile?.status ?: "ACTIVE",
                deletionScheduledAt = currentProfile?.deletionScheduledAt ?: 0L
            )
            repository.saveUserProfile(profile)
        }
    }
}
