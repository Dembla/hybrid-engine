package com.example.hyroxtraining.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface DataRepository {
    fun getWorkouts(): Flow<List<Workout>>
    fun getResults(): Flow<List<WorkoutResult>>
    fun getResultsForWorkout(workoutId: String): Flow<List<WorkoutResult>>
    fun getSchedule(): Flow<List<ScheduleItem>>
    suspend fun addWorkout(workout: Workout): Boolean
    suspend fun deleteWorkout(workoutId: String): Boolean
    suspend fun addResult(result: WorkoutResult): Boolean
    suspend fun deleteResult(resultId: String): Boolean
    suspend fun getWorkoutById(workoutId: String): Workout?
    suspend fun updateScheduleItem(dayIndex: Int, workoutIds: List<String>, isCompleted: Boolean): Boolean
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile): Boolean
    suspend fun deleteUserDataLocally(): Boolean
    suspend fun triggerSync(): Boolean
    suspend fun clearLocalData(): Boolean
}

class DefaultDataRepository(context: Context) : DataRepository {
    private val appContext = context.applicationContext
    private val dbHelper = DatabaseHelper(appContext)

    // Trigger state changes reactively
    private val _workoutUpdateTrigger = MutableStateFlow(System.currentTimeMillis())
    private val _resultUpdateTrigger = MutableStateFlow(System.currentTimeMillis())
    private val _scheduleUpdateTrigger = MutableStateFlow(System.currentTimeMillis())
    private val _profileUpdateTrigger = MutableStateFlow(System.currentTimeMillis())

    init {
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            // Instantly re-evaluate repository flows for the new user ID
            _workoutUpdateTrigger.value = System.currentTimeMillis()
            _resultUpdateTrigger.value = System.currentTimeMillis()
            _scheduleUpdateTrigger.value = System.currentTimeMillis()
            _profileUpdateTrigger.value = System.currentTimeMillis()

            if (currentUser != null) {
                // Trigger background synchronization to pull online data and merge it locally
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    FirebaseSyncHelper.syncData(appContext)
                    // Notify flows again to show newly pulled Firestore data
                    _workoutUpdateTrigger.value = System.currentTimeMillis()
                    _resultUpdateTrigger.value = System.currentTimeMillis()
                    _scheduleUpdateTrigger.value = System.currentTimeMillis()
                    _profileUpdateTrigger.value = System.currentTimeMillis()
                }
            }
        }
    }

    private fun getUserId(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "default"
    }

    override fun getWorkouts(): Flow<List<Workout>> {
        return _workoutUpdateTrigger.map {
            dbHelper.getAllWorkouts(getUserId())
        }
    }

    override fun getResults(): Flow<List<WorkoutResult>> {
        return _resultUpdateTrigger.map {
            dbHelper.getAllResults(getUserId())
        }
    }

    override fun getResultsForWorkout(workoutId: String): Flow<List<WorkoutResult>> {
        return _resultUpdateTrigger.map {
            dbHelper.getResultsForWorkout(workoutId, getUserId())
        }
    }

    override fun getSchedule(): Flow<List<ScheduleItem>> {
        return _scheduleUpdateTrigger.map {
            dbHelper.getScheduleList(getUserId())
        }
    }

    override suspend fun addWorkout(workout: Workout): Boolean {
        val success = dbHelper.insertWorkout(workout, getUserId())
        if (success) {
            _workoutUpdateTrigger.value = System.currentTimeMillis()
            // Asynchronously sync new/edited workout to Firebase
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                FirebaseSyncHelper.syncData(appContext)
            }
        }
        return success
    }

    override suspend fun deleteWorkout(workoutId: String): Boolean {
        val success = dbHelper.deleteWorkout(workoutId)
        if (success) {
            _workoutUpdateTrigger.value = System.currentTimeMillis()
            _resultUpdateTrigger.value = System.currentTimeMillis() // Cascaded deletes
            // Asynchronously delete remote workout from Firebase
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    if (FirebaseSyncHelper.initializeFirebase(appContext)) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(getUserId())
                            .collection("workouts").document(workoutId)
                            .delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return success
    }

    override suspend fun addResult(result: WorkoutResult): Boolean {
        val success = dbHelper.insertWorkoutResult(result, getUserId())
        if (success) {
            _resultUpdateTrigger.value = System.currentTimeMillis()
            // Asynchronously sync new/edited result to Firebase
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                FirebaseSyncHelper.syncData(appContext)
            }
        }
        return success
    }

    override suspend fun deleteResult(resultId: String): Boolean {
        val success = dbHelper.deleteWorkoutResult(resultId)
        if (success) {
            _resultUpdateTrigger.value = System.currentTimeMillis()
            // Asynchronously delete remote result from Firebase
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    if (FirebaseSyncHelper.initializeFirebase(appContext)) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(getUserId())
                            .collection("results").document(resultId)
                            .delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return success
    }

    override suspend fun getWorkoutById(workoutId: String): Workout? {
        return dbHelper.getWorkoutById(workoutId, getUserId())
    }

    override suspend fun updateScheduleItem(dayIndex: Int, workoutIds: List<String>, isCompleted: Boolean): Boolean {
        val success = dbHelper.updateScheduleItem(dayIndex, workoutIds, isCompleted, getUserId())
        if (success) {
            _scheduleUpdateTrigger.value = System.currentTimeMillis()
            // Asynchronously sync schedule edits to Firebase
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                FirebaseSyncHelper.syncData(appContext)
            }
        }
        return success
    }

    override fun getUserProfile(): Flow<UserProfile?> {
        return _profileUpdateTrigger.map {
            dbHelper.getUserProfile(getUserId())
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile): Boolean {
        val success = dbHelper.insertUserProfile(profile)
        if (success) {
            _profileUpdateTrigger.value = System.currentTimeMillis()
            // Asynchronously sync new/edited profile to Firebase
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                FirebaseSyncHelper.syncData(appContext)
            }
        }
        return success
    }

    override suspend fun deleteUserDataLocally(): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                dbHelper.deleteUserDataLocally(getUserId())
                _workoutUpdateTrigger.value = System.currentTimeMillis()
                _resultUpdateTrigger.value = System.currentTimeMillis()
                _scheduleUpdateTrigger.value = System.currentTimeMillis()
                _profileUpdateTrigger.value = System.currentTimeMillis()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override suspend fun triggerSync(): Boolean {
        val success = FirebaseSyncHelper.syncData(appContext)
        if (success) {
            _workoutUpdateTrigger.value = System.currentTimeMillis()
            _resultUpdateTrigger.value = System.currentTimeMillis()
            _scheduleUpdateTrigger.value = System.currentTimeMillis()
            _profileUpdateTrigger.value = System.currentTimeMillis()
        }
        return success
    }

    override suspend fun clearLocalData(): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                dbHelper.clearAllData()
                _workoutUpdateTrigger.value = System.currentTimeMillis()
                _resultUpdateTrigger.value = System.currentTimeMillis()
                _scheduleUpdateTrigger.value = System.currentTimeMillis()
                _profileUpdateTrigger.value = System.currentTimeMillis()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
