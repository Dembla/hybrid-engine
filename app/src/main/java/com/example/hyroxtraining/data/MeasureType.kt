package com.example.hyroxtraining.data

enum class MeasureType(val displayName: String, val unit: String, val placeholder: String) {
    DISTANCE("Distance", "m", "e.g. 1000"),
    TIME("Time", "MM.SS", "e.g. 04.15"),
    AMRAP_REPS("AMRAP - Reps", "reps", "e.g. 45"),
    AMRAP_ROUNDS("AMRAP - Rounds", "rounds", "e.g. 8"),
    AMRAP_ROUNDS_REPS("AMRAP - Rounds and Reps", "rounds + reps", "e.g. 5 + 10"),
    CALORIES("Calories", "kcal", "e.g. 120"),
    CHECKMARK("Checkmark", "", "Completed (Y/N)"),
    EACH_ROUND("Each Round", "reps/sec", "e.g. 12, 10, 11"),
    WEIGHT("Weight", "kg", "e.g. 60"),
    WEIGHTLIFTING("Weightlifting", "kg x reps", "e.g. 60 x 8");

    companion object {
        fun isNumericMeasure(type: MeasureType): Boolean {
            return type == DISTANCE || type == CALORIES || type == WEIGHT || type == AMRAP_REPS || type == AMRAP_ROUNDS
        }

        fun fromDisplayName(name: String): MeasureType {
            return values().firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: DISTANCE
        }

        fun validateAndFormatTime(input: String): Pair<Double, String>? {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return null

            // Simple integer (minutes only, assume 0 seconds)
            if (trimmed.all { it.isDigit() }) {
                val mins = trimmed.toIntOrNull() ?: return null
                val formatted = String.format("%02d.00", mins)
                return Pair(mins.toDouble(), formatted)
            }

            // Decimal MM.SS
            val parts = trimmed.split(".")
            if (parts.size == 2) {
                val minsPart = parts[0]
                val secsPart = parts[1]

                if (minsPart.all { it.isDigit() } && secsPart.all { it.isDigit() } && minsPart.isNotEmpty() && secsPart.isNotEmpty()) {
                    val mins = minsPart.toIntOrNull() ?: return null
                    var secs = secsPart.toIntOrNull() ?: return null
                    
                    if (secsPart.length > 2) return null
                    
                    // If seconds >= 60 and ends with 0 (e.g. 90, 80, 70), convert to single-digit seconds (e.g. 9, 8, 7)
                    if (secs >= 60) {
                        if (secs % 10 == 0) {
                            secs /= 10
                        } else {
                            return null // strictly invalid seconds
                        }
                    }

                    val formatted = String.format("%02d.%02d", mins, secs)
                    val dVal = mins.toDouble() + (secs.toDouble() / 100.0)
                    return Pair(dVal, formatted)
                }
            }

            return null
        }
    }
}
