package com.example.hyroxtraining.data

import kotlinx.serialization.Serializable

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val description: String,
    val isCustom: Boolean,
    val measures: List<MeasureType>,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class MeasureValue(
    val id: String,
    val resultId: String,
    val measureType: MeasureType,
    val doubleValue: Double,
    val stringValue: String
)

@Serializable
data class WorkoutResult(
    val id: String,
    val workoutId: String,
    val timestamp: Long,
    val notes: String = "",
    val values: List<MeasureValue> = emptyList(),
    val workoutName: String = ""
)

@Serializable
data class ScheduleItem(
    val dayName: String,
    val dayIndex: Int,
    val plannedWorkoutIds: List<String> = emptyList(),
    val isCompleted: Boolean = false
)

@Serializable
data class UserProfile(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String, // YYYY-MM-DD
    val country: String,
    val state: String,
    val city: String,
    val pincode: String,
    val status: String = "ACTIVE", // ACTIVE or DORMANT
    val deletionScheduledAt: Long = 0L
)


