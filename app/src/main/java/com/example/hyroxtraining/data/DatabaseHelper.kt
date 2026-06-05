package com.example.hyroxtraining.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "hyrox_training.db"
        private const val DATABASE_VERSION = 8

        // Table workouts
        private const val TABLE_WORKOUTS = "workouts"
        private const val COL_WORKOUT_ID = "id"
        private const val COL_WORKOUT_NAME = "name"
        private const val COL_WORKOUT_DESC = "description"
        private const val COL_WORKOUT_IS_CUSTOM = "is_custom"
        private const val COL_WORKOUT_MEASURES = "measures"
        private const val COL_WORKOUT_CREATED_AT = "created_at"

        // Table workout_results
        private const val TABLE_RESULTS = "workout_results"
        private const val COL_RESULT_ID = "id"
        private const val COL_RESULT_WORKOUT_ID = "workout_id"
        private const val COL_RESULT_TIMESTAMP = "timestamp"
        private const val COL_RESULT_NOTES = "notes"

        // Table measure_values
        private const val TABLE_MEASURES = "measure_values"
        private const val COL_MEASURE_ID = "id"
        private const val COL_MEASURE_RESULT_ID = "result_id"
        private const val COL_MEASURE_TYPE = "measure_type"
        private const val COL_MEASURE_DOUBLE = "double_value"
        private const val COL_MEASURE_STRING = "string_value"

        // New synchronization column names
        const val COL_SYNC_STATUS = "sync_status"
        const val COL_LAST_UPDATED = "last_updated"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create workouts table with sync and user_id columns
        db.execSQL(
            """
            CREATE TABLE $TABLE_WORKOUTS (
                $COL_WORKOUT_ID TEXT NOT NULL,
                $COL_WORKOUT_NAME TEXT NOT NULL,
                $COL_WORKOUT_DESC TEXT,
                $COL_WORKOUT_IS_CUSTOM INTEGER NOT NULL DEFAULT 0,
                $COL_WORKOUT_MEASURES TEXT NOT NULL,
                $COL_WORKOUT_CREATED_AT INTEGER NOT NULL,
                $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'SYNCED',
                $COL_LAST_UPDATED INTEGER NOT NULL DEFAULT 0,
                user_id TEXT NOT NULL DEFAULT 'system',
                PRIMARY KEY ($COL_WORKOUT_ID, user_id)
            )
            """.trimIndent()
        )

        // Create workout_results table with sync and user_id columns (No foreign key referencing workouts to prevent Cascade deletions)
        db.execSQL(
            """
            CREATE TABLE $TABLE_RESULTS (
                $COL_RESULT_ID TEXT PRIMARY KEY,
                $COL_RESULT_WORKOUT_ID TEXT NOT NULL,
                $COL_RESULT_TIMESTAMP INTEGER NOT NULL,
                $COL_RESULT_NOTES TEXT,
                $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'SYNCED',
                $COL_LAST_UPDATED INTEGER NOT NULL DEFAULT 0,
                user_id TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Create measure_values table
        db.execSQL(
            """
            CREATE TABLE $TABLE_MEASURES (
                $COL_MEASURE_ID TEXT PRIMARY KEY,
                $COL_MEASURE_RESULT_ID TEXT NOT NULL,
                $COL_MEASURE_TYPE TEXT NOT NULL,
                $COL_MEASURE_DOUBLE REAL NOT NULL,
                $COL_MEASURE_STRING TEXT NOT NULL,
                FOREIGN KEY($COL_MEASURE_RESULT_ID) REFERENCES $TABLE_RESULTS($COL_RESULT_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Create weekly_schedule table with user_id and day_index compound primary key
        db.execSQL(
            """
            CREATE TABLE weekly_schedule (
                user_id TEXT NOT NULL,
                day_index INTEGER NOT NULL,
                day_name TEXT NOT NULL,
                planned_workout_ids TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                sync_status TEXT NOT NULL DEFAULT 'SYNCED',
                last_updated INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(user_id, day_index)
            )
            """.trimIndent()
        )

        // Create user_profiles table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profiles (
                user_id TEXT PRIMARY KEY,
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL,
                date_of_birth TEXT NOT NULL,
                country TEXT NOT NULL,
                state TEXT NOT NULL,
                city TEXT NOT NULL,
                pincode TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                deletion_scheduled_at INTEGER NOT NULL DEFAULT 0,
                sync_status TEXT NOT NULL DEFAULT 'SYNCED',
                last_updated INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // Seed 8 official HYROX exercises
        seedCoreExercises(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_WORKOUTS ADD COLUMN $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE $TABLE_WORKOUTS ADD COLUMN $COL_LAST_UPDATED INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_RESULTS ADD COLUMN $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE $TABLE_RESULTS ADD COLUMN $COL_LAST_UPDATED INTEGER NOT NULL DEFAULT 0")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_schedule (
                        day_index INTEGER PRIMARY KEY,
                        day_name TEXT NOT NULL,
                        planned_workout_ids TEXT NOT NULL,
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        sync_status TEXT NOT NULL DEFAULT 'SYNCED',
                        last_updated INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                seedDefaultScheduleOld(db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("DROP TABLE IF EXISTS weekly_schedule")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_MEASURES")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_RESULTS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUTS")
                onCreate(db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUTS")
                db.execSQL(
                    """
                    CREATE TABLE $TABLE_WORKOUTS (
                        $COL_WORKOUT_ID TEXT NOT NULL,
                        $COL_WORKOUT_NAME TEXT NOT NULL,
                        $COL_WORKOUT_DESC TEXT,
                        $COL_WORKOUT_IS_CUSTOM INTEGER NOT NULL DEFAULT 0,
                        $COL_WORKOUT_MEASURES TEXT NOT NULL,
                        $COL_WORKOUT_CREATED_AT INTEGER NOT NULL,
                        $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'SYNCED',
                        $COL_LAST_UPDATED INTEGER NOT NULL DEFAULT 0,
                        user_id TEXT NOT NULL DEFAULT 'system',
                        PRIMARY KEY ($COL_WORKOUT_ID, user_id)
                    )
                    """.trimIndent()
                )
                seedCoreExercises(db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 6) {
            try {
                // Drop and recreate TABLE_WORKOUTS to ensure compound key is clean and correct
                db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUTS")
                db.execSQL(
                    """
                    CREATE TABLE $TABLE_WORKOUTS (
                        $COL_WORKOUT_ID TEXT NOT NULL,
                        $COL_WORKOUT_NAME TEXT NOT NULL,
                        $COL_WORKOUT_DESC TEXT,
                        $COL_WORKOUT_IS_CUSTOM INTEGER NOT NULL DEFAULT 0,
                        $COL_WORKOUT_MEASURES TEXT NOT NULL,
                        $COL_WORKOUT_CREATED_AT INTEGER NOT NULL,
                        $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'SYNCED',
                        $COL_LAST_UPDATED INTEGER NOT NULL DEFAULT 0,
                        user_id TEXT NOT NULL DEFAULT 'system',
                        PRIMARY KEY ($COL_WORKOUT_ID, user_id)
                    )
                    """.trimIndent()
                )
                seedCoreExercises(db)

                // Safely migrate TABLE_RESULTS to remove foreign key cascade constraint without losing logs
                db.execSQL("ALTER TABLE $TABLE_RESULTS RENAME TO temp_results")
                db.execSQL(
                    """
                    CREATE TABLE $TABLE_RESULTS (
                        $COL_RESULT_ID TEXT PRIMARY KEY,
                        $COL_RESULT_WORKOUT_ID TEXT NOT NULL,
                        $COL_RESULT_TIMESTAMP INTEGER NOT NULL,
                        $COL_RESULT_NOTES TEXT,
                        $COL_SYNC_STATUS TEXT NOT NULL DEFAULT 'SYNCED',
                        $COL_LAST_UPDATED INTEGER NOT NULL DEFAULT 0,
                        user_id TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO $TABLE_RESULTS SELECT * FROM temp_results")
                db.execSQL("DROP TABLE temp_results")
            } catch (e: Exception) {
                // Fallback: If anything fails, drop and recreate tables to ensure database is perfectly functional
                e.printStackTrace()
                db.execSQL("DROP TABLE IF EXISTS $TABLE_RESULTS")
                db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKOUTS")
                onCreate(db)
            }
        }
        if (oldVersion < 7) {
            try {
                // Safely migrate TABLE_MEASURES to point to the correct TABLE_RESULTS instead of dead temp_results
                db.execSQL("DROP TABLE IF EXISTS temp_measures")
                db.execSQL("ALTER TABLE $TABLE_MEASURES RENAME TO temp_measures")
                db.execSQL(
                    """
                    CREATE TABLE $TABLE_MEASURES (
                        $COL_MEASURE_ID TEXT PRIMARY KEY,
                        $COL_MEASURE_RESULT_ID TEXT NOT NULL,
                        $COL_MEASURE_TYPE TEXT NOT NULL,
                        $COL_MEASURE_DOUBLE REAL NOT NULL,
                        $COL_MEASURE_STRING TEXT NOT NULL,
                        FOREIGN KEY($COL_MEASURE_RESULT_ID) REFERENCES $TABLE_RESULTS($COL_RESULT_ID) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT INTO $TABLE_MEASURES SELECT * FROM temp_measures")
                db.execSQL("DROP TABLE temp_measures")
            } catch (e: Exception) {
                e.printStackTrace()
                db.execSQL("DROP TABLE IF EXISTS $TABLE_MEASURES")
                db.execSQL(
                    """
                    CREATE TABLE $TABLE_MEASURES (
                        $COL_MEASURE_ID TEXT PRIMARY KEY,
                        $COL_MEASURE_RESULT_ID TEXT NOT NULL,
                        $COL_MEASURE_TYPE TEXT NOT NULL,
                        $COL_MEASURE_DOUBLE REAL NOT NULL,
                        $COL_MEASURE_STRING TEXT NOT NULL,
                        FOREIGN KEY($COL_MEASURE_RESULT_ID) REFERENCES $TABLE_RESULTS($COL_RESULT_ID) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }
        if (oldVersion < 8) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profiles (
                        user_id TEXT PRIMARY KEY,
                        first_name TEXT NOT NULL,
                        last_name TEXT NOT NULL,
                        date_of_birth TEXT NOT NULL,
                        country TEXT NOT NULL,
                        state TEXT NOT NULL,
                        city TEXT NOT NULL,
                        pincode TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        deletion_scheduled_at INTEGER NOT NULL DEFAULT 0,
                        sync_status TEXT NOT NULL DEFAULT 'SYNCED',
                        last_updated INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun seedDefaultScheduleOld(db: SQLiteDatabase) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEachIndexed { idx, dayName ->
            val cv = ContentValues().apply {
                put("day_index", idx + 1)
                put("day_name", dayName)
                put("planned_workout_ids", "")
                put("is_completed", 0)
                put("sync_status", "SYNCED")
                put("last_updated", 0)
            }
            db.insert("weekly_schedule", null, cv)
        }
    }

    private fun seedDefaultSchedule(db: SQLiteDatabase, userId: String) {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEachIndexed { idx, dayName ->
            val cv = ContentValues().apply {
                put("user_id", userId)
                put("day_index", idx + 1)
                put("day_name", dayName)
                put("planned_workout_ids", "")
                put("is_completed", 0)
                put("sync_status", "PENDING")
                put("last_updated", System.currentTimeMillis())
            }
            db.insertWithOnConflict("weekly_schedule", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun seedCoreExercises(db: SQLiteDatabase) {
        val coreWorkouts = listOf(
            Workout(
                "core_skierg",
                "SkiErg",
                "1000m SkiErg simulation targeting aerobic capacity and upper body endurance.",
                false,
                listOf(MeasureType.DISTANCE, MeasureType.CALORIES, MeasureType.TIME)
            ),
            Workout(
                "core_sledpush",
                "Sled Push",
                "50m Sled Push (usually 102kg/152kg) testing raw leg drive and mental grit.",
                false,
                listOf(MeasureType.WEIGHT, MeasureType.TIME, MeasureType.DISTANCE)
            ),
            Workout(
                "core_sledpull",
                "Sled Pull",
                "50m Sled Pull (usually 78kg/103kg) engaging posterior chain strength and stability.",
                false,
                listOf(MeasureType.WEIGHT, MeasureType.TIME, MeasureType.DISTANCE)
            ),
            Workout(
                "core_burpeebroadjumps",
                "Burpee Broad Jumps",
                "80m Burpee Broad Jumps, an intensive full-body plyometric endurance exercise.",
                false,
                listOf(MeasureType.DISTANCE, MeasureType.TIME, MeasureType.AMRAP_REPS)
            ),
            Workout(
                "core_rowing",
                "Rowing",
                "1000m Rowing test challenging full-body pacing and cardiovascular output.",
                false,
                listOf(MeasureType.DISTANCE, MeasureType.CALORIES, MeasureType.TIME)
            ),
            Workout(
                "core_farmerscarry",
                "Farmers Carry",
                "200m Farmers Carry (usually 16kg/24kg or 24kg/32kg) testing grip strength and core bracing.",
                false,
                listOf(MeasureType.WEIGHT, MeasureType.DISTANCE, MeasureType.TIME)
            ),
            Workout(
                "core_sandbaglunges",
                "Sandbag Lunges",
                "100m Sandbag Lunges (usually 10kg/20kg/30kg) focusing on quad endurance and balance.",
                false,
                listOf(MeasureType.WEIGHT, MeasureType.DISTANCE, MeasureType.TIME)
            ),
            Workout(
                "core_wallballs",
                "Wall Balls",
                "75 or 100 Wall Balls (usually 4kg/6kg/9kg) targeting lower body thrust and arm power.",
                false,
                listOf(MeasureType.WEIGHT, MeasureType.AMRAP_REPS, MeasureType.TIME)
            )
        )

        for (workout in coreWorkouts) {
            val cv = ContentValues().apply {
                put(COL_WORKOUT_ID, workout.id)
                put(COL_WORKOUT_NAME, workout.name)
                put(COL_WORKOUT_DESC, workout.description)
                put(COL_WORKOUT_IS_CUSTOM, if (workout.isCustom) 1 else 0)
                put(COL_WORKOUT_MEASURES, workout.measures.joinToString(",") { it.name })
                put(COL_WORKOUT_CREATED_AT, workout.createdAt)
                put("user_id", "system")
            }
            db.insert(TABLE_WORKOUTS, null, cv)
        }
    }

    // --- CRUD workouts ---

    fun insertWorkout(
        workout: Workout,
        userId: String,
        syncStatus: String = "PENDING",
        lastUpdated: Long = System.currentTimeMillis()
    ): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_WORKOUT_ID, workout.id)
            put(COL_WORKOUT_NAME, workout.name)
            put(COL_WORKOUT_DESC, workout.description)
            put(COL_WORKOUT_IS_CUSTOM, if (workout.isCustom) 1 else 0)
            put(COL_WORKOUT_MEASURES, workout.measures.joinToString(",") { it.name })
            put(COL_WORKOUT_CREATED_AT, workout.createdAt)
            put(COL_SYNC_STATUS, syncStatus)
            put(COL_LAST_UPDATED, lastUpdated)
            put("user_id", userId)
        }
        val result = db.insertWithOnConflict(TABLE_WORKOUTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun deleteWorkout(workoutId: String): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_WORKOUTS, "$COL_WORKOUT_ID = ?", arrayOf(workoutId))
        return result > 0
    }

    fun getAllWorkouts(userId: String): List<Workout> {
        val workouts = mutableListOf<Workout>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WORKOUTS,
            null,
            "user_id = ? OR (user_id = 'system' AND $COL_WORKOUT_ID NOT IN (SELECT $COL_WORKOUT_ID FROM $TABLE_WORKOUTS WHERE user_id = ?))",
            arrayOf(userId, userId),
            null, null,
            "$COL_WORKOUT_IS_CUSTOM ASC, $COL_WORKOUT_CREATED_AT DESC"
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_NAME)
            val descIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_DESC)
            val customIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_IS_CUSTOM)
            val measuresIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_MEASURES)
            val createdIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_CREATED_AT)

            do {
                val measuresString = cursor.getString(measuresIndex)
                val measuresList = if (measuresString.isEmpty()) {
                    emptyList()
                } else {
                    measuresString.split(",").map { MeasureType.valueOf(it) }
                }

                workouts.add(
                    Workout(
                        id = cursor.getString(idIndex),
                        name = cursor.getString(nameIndex),
                        description = cursor.getString(descIndex) ?: "",
                        isCustom = cursor.getInt(customIndex) == 1,
                        measures = measuresList,
                        createdAt = cursor.getLong(createdIndex)
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
        }
        return workouts
    }

    fun getWorkoutById(workoutId: String, userId: String = "system"): Workout? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WORKOUTS,
            null,
            "$COL_WORKOUT_ID = ? AND (user_id = ? OR user_id = 'system')",
            arrayOf(workoutId, userId),
            null, null,
            "CASE WHEN user_id = '$userId' THEN 0 ELSE 1 END ASC",
            "1"
        )
        var workout: Workout? = null
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_NAME)
            val descIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_DESC)
            val customIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_IS_CUSTOM)
            val measuresIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_MEASURES)
            val createdIndex = cursor.getColumnIndexOrThrow(COL_WORKOUT_CREATED_AT)

            val measuresString = cursor.getString(measuresIndex)
            val measuresList = if (measuresString.isEmpty()) {
                emptyList()
            } else {
                measuresString.split(",").map { MeasureType.valueOf(it) }
            }

            workout = Workout(
                id = cursor.getString(idIndex),
                name = cursor.getString(nameIndex),
                description = cursor.getString(descIndex) ?: "",
                isCustom = cursor.getInt(customIndex) == 1,
                measures = measuresList,
                createdAt = cursor.getLong(createdIndex)
            )
            cursor.close()
        }
        return workout
    }

    // --- CRUD workout results ---

    fun insertWorkoutResult(
        result: WorkoutResult,
        userId: String,
        syncStatus: String = "PENDING",
        lastUpdated: Long = System.currentTimeMillis()
    ): Boolean {
        android.util.Log.d("DatabaseHelper", "insertWorkoutResult called for user: $userId, result: ${result.id}, workout: ${result.workoutId}")
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Explicitly delete any existing result first to satisfy foreign key cascades cleanly
            val deleted = db.delete(TABLE_RESULTS, "$COL_RESULT_ID = ?", arrayOf(result.id))
            android.util.Log.d("DatabaseHelper", "Deleted existing results for ID ${result.id}: $deleted")

            // Insert result core info
            val cvResult = ContentValues().apply {
                put(COL_RESULT_ID, result.id)
                put(COL_RESULT_WORKOUT_ID, result.workoutId)
                put(COL_RESULT_TIMESTAMP, result.timestamp)
                put(COL_RESULT_NOTES, result.notes)
                put(COL_SYNC_STATUS, syncStatus)
                put(COL_LAST_UPDATED, lastUpdated)
                put("user_id", userId)
            }
            val resInsert = db.insert(TABLE_RESULTS, null, cvResult)
            if (resInsert == -1L) {
                android.util.Log.e("DatabaseHelper", "Failed to insert row into TABLE_RESULTS")
                return false
            }

            // Ensure any measures associated are also cleared (redundant due to cascade, but safe)
            db.delete(TABLE_MEASURES, "$COL_MEASURE_RESULT_ID = ?", arrayOf(result.id))

            // Insert measure values
            for (value in result.values) {
                val cvMeasure = ContentValues().apply {
                    put(COL_MEASURE_ID, value.id.ifEmpty { UUID.randomUUID().toString() })
                    put(COL_MEASURE_RESULT_ID, result.id)
                    put(COL_MEASURE_TYPE, value.measureType.name)
                    put(COL_MEASURE_DOUBLE, value.doubleValue)
                    put(COL_MEASURE_STRING, value.stringValue)
                }
                val measureInsert = db.insert(TABLE_MEASURES, null, cvMeasure)
                if (measureInsert == -1L) {
                    android.util.Log.e("DatabaseHelper", "Failed to insert measure row: ${value.measureType.name}")
                    return false
                }
            }

            db.setTransactionSuccessful()
            android.util.Log.d("DatabaseHelper", "Successfully inserted result ${result.id} for user $userId")
            return true
        } catch (e: Exception) {
            android.util.Log.e("DatabaseHelper", "Exception in insertWorkoutResult for result ${result.id}", e)
            e.printStackTrace()
            return false
        } finally {
            db.endTransaction()
        }
    }

    fun getWorkoutLastUpdated(workoutId: String, userId: String = "system"): Long {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WORKOUTS,
            arrayOf(COL_LAST_UPDATED),
            "$COL_WORKOUT_ID = ? AND (user_id = ? OR user_id = 'system')",
            arrayOf(workoutId, userId),
            null, null,
            "CASE WHEN user_id = '$userId' THEN 0 ELSE 1 END ASC",
            "1"
        )
        var ts = 0L
        if (cursor != null && cursor.moveToFirst()) {
            ts = cursor.getLong(0)
            cursor.close()
        }
        return ts
    }

    fun getResultLastUpdated(resultId: String): Long {
        val db = readableDatabase
        val cursor = db.query(TABLE_RESULTS, arrayOf(COL_LAST_UPDATED), "$COL_RESULT_ID = ?", arrayOf(resultId), null, null, null)
        var ts = 0L
        if (cursor != null && cursor.moveToFirst()) {
            ts = cursor.getLong(0)
            cursor.close()
        }
        return ts
    }

    fun getPendingWorkouts(userId: String): List<Pair<Workout, Long>> {
        val list = mutableListOf<Pair<Workout, Long>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_WORKOUTS,
            null,
            "$COL_SYNC_STATUS = ? AND user_id = ?",
            arrayOf("PENDING", userId),
            null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndexOrThrow(COL_WORKOUT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(COL_WORKOUT_NAME)
            val descIdx = cursor.getColumnIndexOrThrow(COL_WORKOUT_DESC)
            val customIdx = cursor.getColumnIndexOrThrow(COL_WORKOUT_IS_CUSTOM)
            val measuresIdx = cursor.getColumnIndexOrThrow(COL_WORKOUT_MEASURES)
            val createdIdx = cursor.getColumnIndexOrThrow(COL_WORKOUT_CREATED_AT)
            val updatedIdx = cursor.getColumnIndexOrThrow(COL_LAST_UPDATED)

            do {
                val measuresString = cursor.getString(measuresIdx)
                val measuresList = if (measuresString.isEmpty()) emptyList() else measuresString.split(",").map { MeasureType.valueOf(it) }
                val w = Workout(
                    id = cursor.getString(idIdx),
                    name = cursor.getString(nameIdx),
                    description = cursor.getString(descIdx) ?: "",
                    isCustom = cursor.getInt(customIdx) == 1,
                    measures = measuresList,
                    createdAt = cursor.getLong(createdIdx)
                )
                val ts = cursor.getLong(updatedIdx)
                list.add(Pair(w, ts))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return list
    }

    fun getPendingResults(userId: String): List<Pair<WorkoutResult, Long>> {
        val list = mutableListOf<Pair<WorkoutResult, Long>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RESULTS,
            null,
            "$COL_SYNC_STATUS = ? AND user_id = ?",
            arrayOf("PENDING", userId),
            null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idIdx = cursor.getColumnIndexOrThrow(COL_RESULT_ID)
            val workoutIdIdx = cursor.getColumnIndexOrThrow(COL_RESULT_WORKOUT_ID)
            val tsIdx = cursor.getColumnIndexOrThrow(COL_RESULT_TIMESTAMP)
            val notesIdx = cursor.getColumnIndexOrThrow(COL_RESULT_NOTES)
            val updatedIdx = cursor.getColumnIndexOrThrow(COL_LAST_UPDATED)

            do {
                val resultId = cursor.getString(idIdx)
                val wId = cursor.getString(workoutIdIdx)
                val wName = getWorkoutById(wId, userId)?.name ?: "Unknown Workout"
                val values = getMeasureValuesForResult(resultId)
                val r = WorkoutResult(
                    id = resultId,
                    workoutId = wId,
                    timestamp = cursor.getLong(tsIdx),
                    notes = cursor.getString(notesIdx) ?: "",
                    values = values,
                    workoutName = wName
                )
                val ts = cursor.getLong(updatedIdx)
                list.add(Pair(r, ts))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return list
    }

    fun markWorkoutSynced(workoutId: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SYNC_STATUS, "SYNCED")
        }
        db.update(TABLE_WORKOUTS, cv, "$COL_WORKOUT_ID = ?", arrayOf(workoutId))
    }

    fun markResultSynced(resultId: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SYNC_STATUS, "SYNCED")
        }
        db.update(TABLE_RESULTS, cv, "$COL_RESULT_ID = ?", arrayOf(resultId))
    }

    fun deleteWorkoutResult(resultId: String): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_RESULTS, "$COL_RESULT_ID = ?", arrayOf(resultId))
        return result > 0
    }

    fun getResultsForWorkout(workoutId: String, userId: String): List<WorkoutResult> {
        val results = mutableListOf<WorkoutResult>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RESULTS,
            null, "workout_id = ? AND user_id = ?", arrayOf(workoutId, userId),
            null, null, "$COL_RESULT_TIMESTAMP DESC"
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_RESULT_ID)
            val workoutIdIndex = cursor.getColumnIndexOrThrow(COL_RESULT_WORKOUT_ID)
            val tsIndex = cursor.getColumnIndexOrThrow(COL_RESULT_TIMESTAMP)
            val notesIndex = cursor.getColumnIndexOrThrow(COL_RESULT_NOTES)

            val wName = getWorkoutById(workoutId, userId)?.name ?: "Unknown Workout"

            do {
                val resultId = cursor.getString(idIndex)
                val valuesList = getMeasureValuesForResult(resultId)

                results.add(
                    WorkoutResult(
                        id = resultId,
                        workoutId = cursor.getString(workoutIdIndex),
                        timestamp = cursor.getLong(tsIndex),
                        notes = cursor.getString(notesIndex) ?: "",
                        values = valuesList,
                        workoutName = wName
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
        }
        return results
    }

    fun getAllResults(userId: String): List<WorkoutResult> {
        android.util.Log.d("DatabaseHelper", "getAllResults called for user: $userId")
        val results = mutableListOf<WorkoutResult>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_RESULTS,
            null, "user_id = ?", arrayOf(userId), null, null, "$COL_RESULT_TIMESTAMP DESC"
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_RESULT_ID)
            val workoutIdIndex = cursor.getColumnIndexOrThrow(COL_RESULT_WORKOUT_ID)
            val tsIndex = cursor.getColumnIndexOrThrow(COL_RESULT_TIMESTAMP)
            val notesIndex = cursor.getColumnIndexOrThrow(COL_RESULT_NOTES)

            do {
                val resultId = cursor.getString(idIndex)
                val wId = cursor.getString(workoutIdIndex)
                val wName = getWorkoutById(wId, userId)?.name ?: "Unknown Workout"
                val valuesList = getMeasureValuesForResult(resultId)

                results.add(
                    WorkoutResult(
                        id = resultId,
                        workoutId = wId,
                        timestamp = cursor.getLong(tsIndex),
                        notes = cursor.getString(notesIndex) ?: "",
                        values = valuesList,
                        workoutName = wName
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
        }
        android.util.Log.d("DatabaseHelper", "getAllResults for user $userId returned ${results.size} items")
        return results
    }

    private fun getMeasureValuesForResult(resultId: String): List<MeasureValue> {
        val values = mutableListOf<MeasureValue>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEASURES,
            null, "$COL_MEASURE_RESULT_ID = ?", arrayOf(resultId),
            null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_MEASURE_ID)
            val typeIndex = cursor.getColumnIndexOrThrow(COL_MEASURE_TYPE)
            val doubleIndex = cursor.getColumnIndexOrThrow(COL_MEASURE_DOUBLE)
            val stringIndex = cursor.getColumnIndexOrThrow(COL_MEASURE_STRING)

            do {
                values.add(
                    MeasureValue(
                        id = cursor.getString(idIndex),
                        resultId = resultId,
                        measureType = MeasureType.valueOf(cursor.getString(typeIndex)),
                        doubleValue = cursor.getDouble(doubleIndex),
                        stringValue = cursor.getString(stringIndex)
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
        }
        return values
    }

    private fun getStartOfCurrentWeekMillis(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val daysToSubtract = when (dayOfWeek) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> 0
        }
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysToSubtract)
        return calendar.timeInMillis
    }

    private fun getTodayIndex(): Int {
        val calendar = java.util.Calendar.getInstance()
        val calendarDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        return when (calendarDay) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            java.util.Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun getScheduleList(userId: String): List<ScheduleItem> {
        val db = readableDatabase

        // Dynamically seed schedule if not seeded for this user yet
        val cursorCheck = db.rawQuery("SELECT COUNT(*) FROM weekly_schedule WHERE user_id = ?", arrayOf(userId))
        var count = 0
        if (cursorCheck != null && cursorCheck.moveToFirst()) {
            count = cursorCheck.getInt(0)
            cursorCheck.close()
        }
        if (count == 0) {
            seedDefaultSchedule(writableDatabase, userId)
        }

        val startOfWeek = getStartOfCurrentWeekMillis()
        val todayIdx = getTodayIndex()

        val list = mutableListOf<ScheduleItem>()
        val cursor = db.query(
            "weekly_schedule",
            null, "user_id = ?", arrayOf(userId), null, null,
            "day_index ASC"
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idxColumn = cursor.getColumnIndexOrThrow("day_index")
            val nameColumn = cursor.getColumnIndexOrThrow("day_name")
            val workoutsColumn = cursor.getColumnIndexOrThrow("planned_workout_ids")
            val completedColumn = cursor.getColumnIndexOrThrow("is_completed")
            val lastUpdatedColumn = cursor.getColumnIndexOrThrow(COL_LAST_UPDATED)

            do {
                val dayIndex = cursor.getInt(idxColumn)
                val workoutsString = cursor.getString(workoutsColumn)
                val workoutsList = if (workoutsString.isEmpty()) emptyList() else workoutsString.split(",")
                var isCompleted = cursor.getInt(completedColumn) == 1
                val lastUpdated = cursor.getLong(lastUpdatedColumn)

                // Rollover: If the item was marked completed in a previous week, reset it!
                if (isCompleted && lastUpdated < startOfWeek) {
                    isCompleted = false
                    val cv = ContentValues().apply {
                        put("is_completed", 0)
                        put(COL_SYNC_STATUS, "PENDING")
                        put(COL_LAST_UPDATED, System.currentTimeMillis())
                    }
                    writableDatabase.update("weekly_schedule", cv, "day_index = ? AND user_id = ?", arrayOf(dayIndex.toString(), userId))
                }

                // Future Day check: A future day cannot be marked completed
                if (dayIndex > todayIdx && isCompleted) {
                    isCompleted = false
                    val cv = ContentValues().apply {
                        put("is_completed", 0)
                        put(COL_SYNC_STATUS, "PENDING")
                        put(COL_LAST_UPDATED, System.currentTimeMillis())
                    }
                    writableDatabase.update("weekly_schedule", cv, "day_index = ? AND user_id = ?", arrayOf(dayIndex.toString(), userId))
                }

                list.add(
                    ScheduleItem(
                        dayName = cursor.getString(nameColumn),
                        dayIndex = dayIndex,
                        plannedWorkoutIds = workoutsList,
                        isCompleted = isCompleted
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
        }
        return list
    }


    fun updateScheduleItem(
        dayIndex: Int,
        workoutIds: List<String>,
        isCompleted: Boolean,
        userId: String,
        syncStatus: String = "PENDING",
        lastUpdated: Long = System.currentTimeMillis()
    ): Boolean {
        val db = writableDatabase
        // Trigger seeding if not present
        getScheduleList(userId)
        val cv = ContentValues().apply {
            put("planned_workout_ids", workoutIds.joinToString(","))
            put("is_completed", if (isCompleted) 1 else 0)
            put(COL_SYNC_STATUS, syncStatus)
            put(COL_LAST_UPDATED, lastUpdated)
        }
        val result = db.update("weekly_schedule", cv, "day_index = ? AND user_id = ?", arrayOf(dayIndex.toString(), userId))
        return result > 0
    }

    fun getScheduleItemLastUpdated(dayIndex: Int, userId: String): Long {
        val db = readableDatabase
        val cursor = db.query("weekly_schedule", arrayOf(COL_LAST_UPDATED), "day_index = ? AND user_id = ?", arrayOf(dayIndex.toString(), userId), null, null, null)
        var ts = 0L
        if (cursor != null && cursor.moveToFirst()) {
            ts = cursor.getLong(0)
            cursor.close()
        }
        return ts
    }

    fun getPendingScheduleItems(userId: String): List<Pair<ScheduleItem, Long>> {
        val list = mutableListOf<Pair<ScheduleItem, Long>>()
        val db = readableDatabase
        val cursor = db.query(
            "weekly_schedule",
            null,
            "$COL_SYNC_STATUS = ? AND user_id = ?",
            arrayOf("PENDING", userId),
            null, null, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val idxColumn = cursor.getColumnIndexOrThrow("day_index")
            val nameColumn = cursor.getColumnIndexOrThrow("day_name")
            val workoutsColumn = cursor.getColumnIndexOrThrow("planned_workout_ids")
            val completedColumn = cursor.getColumnIndexOrThrow("is_completed")
            val updatedColumn = cursor.getColumnIndexOrThrow(COL_LAST_UPDATED)

            do {
                val workoutsString = cursor.getString(workoutsColumn)
                val workoutsList = if (workoutsString.isEmpty()) emptyList() else workoutsString.split(",")
                val item = ScheduleItem(
                    dayName = cursor.getString(nameColumn),
                    dayIndex = cursor.getInt(idxColumn),
                    plannedWorkoutIds = workoutsList,
                    isCompleted = cursor.getInt(completedColumn) == 1
                )
                val ts = cursor.getLong(updatedColumn)
                list.add(Pair(item, ts))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return list
    }

    fun markScheduleItemSynced(dayIndex: Int, userId: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SYNC_STATUS, "SYNCED")
        }
        db.update("weekly_schedule", cv, "day_index = ? AND user_id = ?", arrayOf(dayIndex.toString(), userId))
    }

    fun clearAllData() {
        // No-op: Local SQLite data is fully user-partitioned and is preserved on logout.
    }

    private fun createUserProfilesTableDb(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profiles (
                user_id TEXT PRIMARY KEY,
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL,
                date_of_birth TEXT NOT NULL,
                country TEXT NOT NULL,
                state TEXT NOT NULL,
                city TEXT NOT NULL,
                pincode TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                deletion_scheduled_at INTEGER NOT NULL DEFAULT 0,
                sync_status TEXT NOT NULL DEFAULT 'SYNCED',
                last_updated INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    fun insertUserProfile(
        profile: UserProfile,
        syncStatus: String = "PENDING",
        lastUpdated: Long = System.currentTimeMillis()
    ): Boolean {
        try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("user_id", profile.userId)
                put("first_name", profile.firstName)
                put("last_name", profile.lastName)
                put("date_of_birth", profile.dateOfBirth)
                put("country", profile.country)
                put("state", profile.state)
                put("city", profile.city)
                put("pincode", profile.pincode)
                put("status", profile.status)
                put("deletion_scheduled_at", profile.deletionScheduledAt)
                put(COL_SYNC_STATUS, syncStatus)
                put(COL_LAST_UPDATED, lastUpdated)
            }
            val result = db.insertWithOnConflict("user_profiles", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            return result != -1L
        } catch (e: android.database.sqlite.SQLiteException) {
            e.printStackTrace()
            try {
                createUserProfilesTableDb(writableDatabase)
                val db = writableDatabase
                val cv = ContentValues().apply {
                    put("user_id", profile.userId)
                    put("first_name", profile.firstName)
                    put("last_name", profile.lastName)
                    put("date_of_birth", profile.dateOfBirth)
                    put("country", profile.country)
                    put("state", profile.state)
                    put("city", profile.city)
                    put("pincode", profile.pincode)
                    put("status", profile.status)
                    put("deletion_scheduled_at", profile.deletionScheduledAt)
                    put(COL_SYNC_STATUS, syncStatus)
                    put(COL_LAST_UPDATED, lastUpdated)
                }
                val result = db.insertWithOnConflict("user_profiles", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                return result != -1L
            } catch (ex: Exception) {
                ex.printStackTrace()
                return false
            }
        }
    }

    fun getUserProfile(userId: String): UserProfile? {
        try {
            val db = readableDatabase
            val cursor = db.query(
                "user_profiles",
                null,
                "user_id = ?",
                arrayOf(userId),
                null, null, null
            )
            var profile: UserProfile? = null
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    profile = UserProfile(
                        userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
                        firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                        lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                        dateOfBirth = cursor.getString(cursor.getColumnIndexOrThrow("date_of_birth")),
                        country = cursor.getString(cursor.getColumnIndexOrThrow("country")),
                        state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                        city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                        pincode = cursor.getString(cursor.getColumnIndexOrThrow("pincode")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        deletionScheduledAt = cursor.getLong(cursor.getColumnIndexOrThrow("deletion_scheduled_at"))
                    )
                }
                cursor.close()
            }
            return profile
        } catch (e: android.database.sqlite.SQLiteException) {
            e.printStackTrace()
            try {
                createUserProfilesTableDb(writableDatabase)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return null
        }
    }

    fun getUserProfileLastUpdated(userId: String): Long {
        try {
            val db = readableDatabase
            val cursor = db.query(
                "user_profiles",
                arrayOf(COL_LAST_UPDATED),
                "user_id = ?",
                arrayOf(userId),
                null, null, null
            )
            var ts = 0L
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    ts = cursor.getLong(0)
                }
                cursor.close()
            }
            return ts
        } catch (e: android.database.sqlite.SQLiteException) {
            e.printStackTrace()
            try {
                createUserProfilesTableDb(writableDatabase)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return 0L
        }
    }

    fun getPendingUserProfile(userId: String): Pair<UserProfile, Long>? {
        try {
            val db = readableDatabase
            val cursor = db.query(
                "user_profiles",
                null,
                "$COL_SYNC_STATUS = ? AND user_id = ?",
                arrayOf("PENDING", userId),
                null, null, null
            )
            var result: Pair<UserProfile, Long>? = null
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val profile = UserProfile(
                        userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
                        firstName = cursor.getString(cursor.getColumnIndexOrThrow("first_name")),
                        lastName = cursor.getString(cursor.getColumnIndexOrThrow("last_name")),
                        dateOfBirth = cursor.getString(cursor.getColumnIndexOrThrow("date_of_birth")),
                        country = cursor.getString(cursor.getColumnIndexOrThrow("country")),
                        state = cursor.getString(cursor.getColumnIndexOrThrow("state")),
                        city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                        pincode = cursor.getString(cursor.getColumnIndexOrThrow("pincode")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        deletionScheduledAt = cursor.getLong(cursor.getColumnIndexOrThrow("deletion_scheduled_at"))
                    )
                    val ts = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_UPDATED))
                    result = Pair(profile, ts)
                }
                cursor.close()
            }
            return result
        } catch (e: android.database.sqlite.SQLiteException) {
            e.printStackTrace()
            try {
                createUserProfilesTableDb(writableDatabase)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return null
        }
    }

    fun markUserProfileSynced(userId: String) {
        try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(COL_SYNC_STATUS, "SYNCED")
            }
            db.update("user_profiles", cv, "user_id = ?", arrayOf(userId))
        } catch (e: android.database.sqlite.SQLiteException) {
            e.printStackTrace()
            try {
                createUserProfilesTableDb(writableDatabase)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun deleteUserDataLocally(userId: String) {
        val db = writableDatabase
        db.delete("workouts", "user_id = ?", arrayOf(userId))
        db.delete("workout_results", "user_id = ?", arrayOf(userId))
        db.delete("weekly_schedule", "user_id = ?", arrayOf(userId))
        try {
            db.delete("user_profiles", "user_id = ?", arrayOf(userId))
        } catch (e: android.database.sqlite.SQLiteException) {
            e.printStackTrace()
            try {
                createUserProfilesTableDb(writableDatabase)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
