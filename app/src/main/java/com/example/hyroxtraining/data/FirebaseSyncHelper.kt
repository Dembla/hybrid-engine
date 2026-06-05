package com.example.hyroxtraining.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class SyncState {
    IDLE,
    SYNCING,
    COMPLETED,
    FAILED,
    OFFLINE_NO_CONFIG
}

object FirebaseSyncHelper {
    private const val TAG = "FirebaseSyncHelper"
    private const val PREFS_NAME = "firebase_sync_prefs"
    private const val KEY_ENABLED = "firebase_enabled"
    private const val KEY_API_KEY = "firebase_api_key"
    private const val KEY_PROJECT_ID = "firebase_project_id"
    private const val KEY_APP_ID = "firebase_app_id"
    private const val DEFAULT_USER_ID = "hyrox_athlete"

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState = _syncState.asStateFlow()

    private val _syncMessage = MutableStateFlow("Tap Sync to Backup Data")
    val syncMessage = _syncMessage.asStateFlow()

    private var isFirebaseInitialized = false
    private var firestoreInstance: FirebaseFirestore? = null

    fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(context: Context): Boolean {
        return true
    }

    fun getCredentials(context: Context): Triple<String, String, String> {
        val prefs = getPrefs(context)
        return Triple(
            prefs.getString(KEY_PROJECT_ID, "") ?: "",
            prefs.getString(KEY_API_KEY, "") ?: "",
            prefs.getString(KEY_APP_ID, "") ?: ""
        )
    }

    fun saveCredentials(context: Context, enabled: Boolean, projectId: String, apiKey: String, appId: String) {
        getPrefs(context).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_PROJECT_ID, projectId.trim())
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_APP_ID, appId.trim())
            .apply()
        // Reset initialization so it tries to re-initialize next sync
        isFirebaseInitialized = false
        firestoreInstance = null
    }

    @Synchronized
    fun initializeFirebase(context: Context): Boolean {
        if (isFirebaseInitialized && firestoreInstance != null) return true

        val contextApp = context.applicationContext

        if (!isEnabled(contextApp)) {
            _syncState.value = SyncState.OFFLINE_NO_CONFIG
            _syncMessage.value = "Offline Mode (Configure Sync in Settings)"
            return false
        }

        try {
            // Try standard automatic initialization from google-services.json
            if (FirebaseApp.getApps(contextApp).isEmpty()) {
                FirebaseApp.initializeApp(contextApp)
            }
            firestoreInstance = FirebaseFirestore.getInstance()
            isFirebaseInitialized = true
            _syncState.value = SyncState.IDLE
            _syncMessage.value = "Cloud Backup Active"
            return true
        } catch (e: Exception) {
            // Fallback to manual preferences credentials if standard init fails
            val (projectId, apiKey, appId) = getCredentials(contextApp)
            if (projectId.isNotBlank() && apiKey.isNotBlank() && appId.isNotBlank()) {
                try {
                    if (FirebaseApp.getApps(contextApp).isEmpty()) {
                        val options = FirebaseOptions.Builder()
                            .setProjectId(projectId)
                            .setApiKey(apiKey)
                            .setApplicationId(appId)
                            .build()
                        FirebaseApp.initializeApp(contextApp, options)
                    }
                    firestoreInstance = FirebaseFirestore.getInstance()
                    isFirebaseInitialized = true
                    _syncState.value = SyncState.IDLE
                    _syncMessage.value = "Cloud Backup Active"
                    return true
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed manual credentials initialize", ex)
                }
            }
            Log.e(TAG, "Failed standard initializeFirebase", e)
            _syncState.value = SyncState.FAILED
            _syncMessage.value = "Initialization Failed: ${e.localizedMessage}"
            return false
        }
    }

    suspend fun syncData(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        if (!initializeFirebase(appCtx)) return@withContext false

        val db = firestoreInstance ?: return@withContext false
        val dbHelper = DatabaseHelper(appCtx)

        // Dynamically get the authenticated user's unique ID, fallback to default ID
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: DEFAULT_USER_ID

        _syncState.value = SyncState.SYNCING
        _syncMessage.value = "Syncing with Cloud..."

        try {
            // --- 1. PUSH PENDING LOCAL WORKOUTS ---
            val pendingWorkouts = dbHelper.getPendingWorkouts(userId)
            for ((workout, timestamp) in pendingWorkouts) {
                val workoutDoc = mapOf(
                    "id" to workout.id,
                    "name" to workout.name,
                    "description" to workout.description,
                    "isCustom" to workout.isCustom,
                    "measures" to workout.measures.map { it.name },
                    "createdAt" to workout.createdAt,
                    "lastUpdated" to timestamp
                )
                db.collection("users").document(userId)
                    .collection("workouts").document(workout.id)
                    .set(workoutDoc).await()
                dbHelper.markWorkoutSynced(workout.id)
            }

            // --- 2. PUSH PENDING LOCAL RESULTS ---
            val pendingResults = dbHelper.getPendingResults(userId)
            for ((result, timestamp) in pendingResults) {
                val resultDoc = mapOf(
                    "id" to result.id,
                    "workoutId" to result.workoutId,
                    "workoutName" to result.workoutName,
                    "name" to result.workoutName, // Added for easiest analytics later
                    "timestamp" to result.timestamp,
                    "notes" to result.notes,
                    "lastUpdated" to timestamp,
                    "values" to result.values.map { v ->
                        mapOf(
                            "id" to v.id,
                            "measureType" to v.measureType.name,
                            "doubleValue" to v.doubleValue,
                            "stringValue" to v.stringValue
                        )
                    }
                )
                db.collection("users").document(userId)
                    .collection("results").document(result.id)
                    .set(resultDoc).await()
                dbHelper.markResultSynced(result.id)
            }

            // --- 2.5. PUSH PENDING LOCAL SCHEDULE ITEMS ---
            val pendingSchedule = dbHelper.getPendingScheduleItems(userId)
            for ((item, timestamp) in pendingSchedule) {
                val scheduleDoc = mapOf(
                    "dayIndex" to item.dayIndex,
                    "dayName" to item.dayName,
                    "plannedWorkoutIds" to item.plannedWorkoutIds,
                    "isCompleted" to item.isCompleted,
                    "lastUpdated" to timestamp
                )
                db.collection("users").document(userId)
                    .collection("schedule").document(item.dayIndex.toString())
                    .set(scheduleDoc).await()
                dbHelper.markScheduleItemSynced(item.dayIndex, userId)
            }

            // --- 2.7. PUSH PENDING LOCAL USER PROFILE ---
            val pendingProfilePair = dbHelper.getPendingUserProfile(userId)
            if (pendingProfilePair != null) {
                val (profile, timestamp) = pendingProfilePair
                val profileDoc = mapOf(
                    "userId" to profile.userId,
                    "firstName" to profile.firstName,
                    "lastName" to profile.lastName,
                    "dateOfBirth" to profile.dateOfBirth,
                    "country" to profile.country,
                    "state" to profile.state,
                    "city" to profile.city,
                    "pincode" to profile.pincode,
                    "status" to profile.status,
                    "deletionScheduledAt" to profile.deletionScheduledAt,
                    "lastUpdated" to timestamp
                )
                db.collection("users").document(userId)
                    .collection("profile").document("main")
                    .set(profileDoc).await()
                dbHelper.markUserProfileSynced(userId)
            }

            // --- 3. PULL REMOTE WORKOUTS & MERGE ---
            val remoteWorkoutsSnapshot = db.collection("users").document(userId)
                .collection("workouts").get().await()

            for (doc in remoteWorkoutsSnapshot.documents) {
                val id = doc.getString("id") ?: continue
                val name = doc.getString("name") ?: ""
                val desc = doc.getString("description") ?: ""
                val isCustom = doc.getBoolean("isCustom") ?: true
                val measuresRaw = doc.get("measures") as? List<String> ?: emptyList()
                val measures = measuresRaw.map { MeasureType.valueOf(it) }
                val createdAt = doc.getLong("createdAt") ?: 0L
                val remoteUpdated = doc.getLong("lastUpdated") ?: 0L

                val workout = Workout(id, name, desc, isCustom, measures, createdAt)
                val localUpdated = dbHelper.getWorkoutLastUpdated(id, userId)

                if (remoteUpdated > localUpdated) {
                    dbHelper.insertWorkout(workout, userId = userId, syncStatus = "SYNCED", lastUpdated = remoteUpdated)
                }
            }

            // --- 4. PULL REMOTE RESULTS & MERGE ---
            val remoteResultsSnapshot = db.collection("users").document(userId)
                .collection("results").get().await()

            for (doc in remoteResultsSnapshot.documents) {
                val id = doc.getString("id") ?: continue
                val workoutId = doc.getString("workoutId") ?: ""
                val workoutName = doc.getString("workoutName") ?: doc.getString("name") ?: ""
                val timestamp = doc.getLong("timestamp") ?: 0L
                val notes = doc.getString("notes") ?: ""
                val remoteUpdated = doc.getLong("lastUpdated") ?: 0L

                val valuesRaw = doc.get("values") as? List<Map<String, Any>> ?: emptyList()
                val values = valuesRaw.map { v ->
                    MeasureValue(
                        id = v["id"] as? String ?: "",
                        resultId = id,
                        measureType = MeasureType.valueOf(v["measureType"] as? String ?: "TIME"),
                        doubleValue = (v["doubleValue"] as? Number)?.toDouble() ?: 0.0,
                        stringValue = v["stringValue"] as? String ?: ""
                    )
                }

                val resultObj = WorkoutResult(
                    id = id,
                    workoutId = workoutId,
                    timestamp = timestamp,
                    notes = notes,
                    values = values,
                    workoutName = workoutName
                )
                val localUpdated = dbHelper.getResultLastUpdated(id)

                if (remoteUpdated > localUpdated) {
                    dbHelper.insertWorkoutResult(resultObj, userId = userId, syncStatus = "SYNCED", lastUpdated = remoteUpdated)
                }
            }

            // --- 4.5. PULL REMOTE SCHEDULE ITEMS & MERGE ---
            val remoteScheduleSnapshot = db.collection("users").document(userId)
                .collection("schedule").get().await()

            for (doc in remoteScheduleSnapshot.documents) {
                val dayIndex = doc.getLong("dayIndex")?.toInt() ?: continue
                val plannedWorkoutIds = doc.get("plannedWorkoutIds") as? List<String> ?: emptyList()
                val isCompleted = doc.getBoolean("isCompleted") ?: false
                val remoteUpdated = doc.getLong("lastUpdated") ?: 0L

                val localUpdated = dbHelper.getScheduleItemLastUpdated(dayIndex, userId)

                if (remoteUpdated > localUpdated) {
                    dbHelper.updateScheduleItem(
                        dayIndex = dayIndex,
                        workoutIds = plannedWorkoutIds,
                        isCompleted = isCompleted,
                        userId = userId,
                        syncStatus = "SYNCED",
                        lastUpdated = remoteUpdated
                    )
                }
            }

            // --- 4.7. PULL REMOTE USER PROFILE & MERGE ---
            val remoteProfileDoc = db.collection("users").document(userId)
                .collection("profile").document("main").get().await()

            if (remoteProfileDoc.exists()) {
                val remoteUpdated = remoteProfileDoc.getLong("lastUpdated") ?: 0L
                val localUpdated = dbHelper.getUserProfileLastUpdated(userId)

                if (remoteUpdated > localUpdated) {
                    val profileObj = UserProfile(
                        userId = userId,
                        firstName = remoteProfileDoc.getString("firstName") ?: "",
                        lastName = remoteProfileDoc.getString("lastName") ?: "",
                        dateOfBirth = remoteProfileDoc.getString("dateOfBirth") ?: "",
                        country = remoteProfileDoc.getString("country") ?: "",
                        state = remoteProfileDoc.getString("state") ?: "",
                        city = remoteProfileDoc.getString("city") ?: "",
                        pincode = remoteProfileDoc.getString("pincode") ?: "",
                        status = remoteProfileDoc.getString("status") ?: "ACTIVE",
                        deletionScheduledAt = remoteProfileDoc.getLong("deletionScheduledAt") ?: 0L
                    )
                    dbHelper.insertUserProfile(profileObj, syncStatus = "SYNCED", lastUpdated = remoteUpdated)
                }
            }

            _syncState.value = SyncState.COMPLETED
            _syncMessage.value = "Cloud Sync Succeeded"
            true
        } catch (e: Exception) {
            Log.e(TAG, "Data Sync Error during replication", e)
            _syncState.value = SyncState.FAILED
            _syncMessage.value = "Sync Failed: ${e.localizedMessage}"
            false
        }
    }
}
