package com.example.hyroxtraining.ui.results

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import java.util.UUID
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hyroxtraining.data.MeasureType
import com.example.hyroxtraining.data.MeasureValue
import com.example.hyroxtraining.data.Workout
import com.example.hyroxtraining.data.WorkoutResult
import com.example.hyroxtraining.ui.components.LineChart
import com.example.hyroxtraining.ui.main.MainScreenViewModel
import java.util.Date

@Composable
fun ResultsScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedWorkoutId by remember(state.results) {
        mutableStateOf(state.results.firstOrNull()?.workoutId ?: "core_skierg")
    }
    var selectedMeasureType by remember(selectedWorkoutId) { mutableStateOf<MeasureType?>(null) }
    var showWorkoutSelector by remember { mutableStateOf(false) }
    var showMeasureSelector by remember { mutableStateOf(false) }
    var editingResult by remember { mutableStateOf<WorkoutResult?>(null) }
    var resultToDelete by remember { mutableStateOf<WorkoutResult?>(null) }
    var shareTextToConfirm by remember { mutableStateOf<String?>(null) }

    fun getFormattedValue(valObj: MeasureValue): String {
        return if (valObj.measureType == MeasureType.WEIGHT || valObj.measureType == MeasureType.WEIGHTLIFTING) {
            valObj.stringValue
        } else {
            "${valObj.stringValue} ${valObj.measureType.unit}".trim()
        }
    }

    val selectedWorkout = state.workouts.find { it.id == selectedWorkoutId }
        ?: state.workouts.firstOrNull()

    // Initialize measure type once workout is loaded
    if (selectedWorkout != null && (selectedMeasureType == null || !selectedWorkout.measures.contains(selectedMeasureType))) {
        selectedMeasureType = selectedWorkout.measures.firstOrNull()
    }

    val workoutLogs = state.results
        .filter { it.workoutId == selectedWorkout?.id }
        .sortedBy { it.timestamp } // Sort chronologically for chart display

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PERFORMANCE ANALYTICS",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.results.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Once you log workout results, dynamic line charts and benchmark progressions will visualize your performance trends here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            return
        }

        // Dropdown selectors for Workout & Measure Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Workout Selector Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showWorkoutSelector = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("EXERCISE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        selectedWorkout?.name ?: "Select Exercise",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Measure Selector Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showMeasureSelector = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("METRIC", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        selectedMeasureType?.displayName ?: "Select Metric",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Chart display
        val logsWithSelectedMeasure = workoutLogs.filter { res ->
            res.values.any { it.measureType == selectedMeasureType }
        }
        val filteredValues = logsWithSelectedMeasure.flatMap { res ->
            res.values.filter { it.measureType == selectedMeasureType }
        }

        val dataPoints = filteredValues.map { it.doubleValue }
        val dateLabels = logsWithSelectedMeasure.map { DateFormat.format("MM/dd", Date(it.timestamp)).toString() }

        Text(
            text = "${selectedWorkout?.name ?: ""} PROGRESS TREND",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LineChart(
            dataPoints = dataPoints,
            labels = dateLabels,
            lineColor = MaterialTheme.colorScheme.primary,
            gradientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Benchmark Highlights Panel
        if (dataPoints.size >= 1) {
            val startVal = dataPoints.first()
            val latestVal = dataPoints.last()
            val bestVal = if (selectedMeasureType == MeasureType.TIME) {
                dataPoints.minOrNull() ?: latestVal
            } else {
                dataPoints.maxOrNull() ?: latestVal
            }

            val startString = filteredValues.first().stringValue
            val latestString = filteredValues.last().stringValue
            val bestString = if (selectedMeasureType == MeasureType.TIME) {
                filteredValues.minByOrNull { it.doubleValue }?.stringValue ?: latestString
            } else {
                filteredValues.maxByOrNull { it.doubleValue }?.stringValue ?: latestString
            }

            val isWeightMeasure = selectedMeasureType == MeasureType.WEIGHT || selectedMeasureType == MeasureType.WEIGHTLIFTING
            val displayStart = if (isWeightMeasure) startString else "$startString ${selectedMeasureType?.unit ?: ""}".trim()
            val displayLatest = if (isWeightMeasure) latestString else "$latestString ${selectedMeasureType?.unit ?: ""}".trim()
            val displayBest = if (isWeightMeasure) bestString else "$bestString ${selectedMeasureType?.unit ?: ""}".trim()

            Text(
                text = "TRAINING BENCHMARKS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("INITIAL BENCHMARK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(displayStart, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("CURRENT SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(displayLatest, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PERSONAL BEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "PB", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(displayBest, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }

                        // Progression summary description
                        val isTime = selectedMeasureType == MeasureType.TIME
                        val improved = if (isTime) latestVal < startVal else latestVal > startVal
                        val diffVal = Math.abs(latestVal - startVal)

                        Box(
                            modifier = Modifier
                                .background(
                                    if (improved) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (improved) "IMPROVED" else "MAINTAINING",
                                color = if (improved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // History logs header
        Text(
            text = "WORKOUT HISTORY LOGS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        val allResults = state.results
        if (allResults.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Log workout details to see your complete history here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            allResults.forEach { result ->
                val workoutObj = state.workouts.find { it.id == result.workoutId } ?: return@forEach
                val dateString = DateFormat.format("MMM dd, yyyy - hh:mm a", Date(result.timestamp)).toString()
                val isCurrentlySelected = selectedWorkoutId == result.workoutId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            selectedWorkoutId = result.workoutId
                            selectedMeasureType = null // Auto-initialize to first metric
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentlySelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (isCurrentlySelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = workoutObj.name.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurrentlySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateString.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingResult = result },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Log",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val twoHoursMs = 2L * 60 * 60 * 1000
                                        val clubbedResults = state.results.filter { res ->
                                            Math.abs(res.timestamp - result.timestamp) <= twoHoursMs
                                        }.sortedBy { it.timestamp }

                                        val headerText = "Just crushed the HYROX training workout\n"
                                        val footerText = "\n\nLogged using Hyrox engine"

                                        val workoutBlocks = clubbedResults.map { clubbedRes ->
                                            val wObj = state.workouts.find { it.id == clubbedRes.workoutId }
                                            val wName = wObj?.name ?: "HYROX Workout"
                                            val valsSummary = clubbedRes.values.joinToString("\n") { valObj ->
                                                "${valObj.measureType.displayName}: ${getFormattedValue(valObj)}"
                                            }
                                            val nSummary = if (clubbedRes.notes.isNotEmpty()) "\nNotes: ${clubbedRes.notes}" else ""
                                            "$wName\n$valsSummary$nSummary"
                                        }.joinToString("\n\n")

                                        val shareText = "$headerText$workoutBlocks$footerText"

                                        if (clubbedResults.size > 1) {
                                            shareTextToConfirm = shareText
                                        } else {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "My HYROX Workout Results")
                                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Share Workout Result via"))
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share Log",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { resultToDelete = result },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Log",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (isCurrentlySelected) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "ACTIVE GRAPH VIEW",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Show tracked values
                        result.values.forEach { valObj ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = valObj.measureType.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = getFormattedValue(valObj),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (result.notes.isNotEmpty()) {
                            Divider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                text = "Notes: ${result.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for Workout Selection
    if (showWorkoutSelector) {
        AlertDialog(
            onDismissRequest = { showWorkoutSelector = false },
            title = { Text("Select Exercise", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    state.workouts.forEach { w ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedWorkoutId = w.id
                                    selectedMeasureType = null // Reset measure selection to auto-init
                                    showWorkoutSelector = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedWorkoutId == w.id) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(w.name, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { showWorkoutSelector = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("CLOSE")
                }
            }
        )
    }

    // Dialog for Measure Selection
    if (showMeasureSelector) {
        AlertDialog(
            onDismissRequest = { showMeasureSelector = false },
            title = { Text("Select Metric", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    selectedWorkout?.measures?.forEach { m ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedMeasureType = m
                                    showMeasureSelector = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedMeasureType == m) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(m.displayName, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { showMeasureSelector = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("CLOSE")
                }
            }
        )
    }

    if (resultToDelete != null) {
        val res = resultToDelete!!
        val workoutObj = state.workouts.find { it.id == res.workoutId }
        val workoutName = workoutObj?.name ?: "Workout"
        val dateString = DateFormat.format("MMM dd, yyyy", java.util.Date(res.timestamp)).toString()

        AlertDialog(
            onDismissRequest = { resultToDelete = null },
            title = {
                Text(
                    text = "Delete Log",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the logged result for $workoutName on $dateString?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteResult(res.id)
                        resultToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { resultToDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("No")
                }
            }
        )
    }

    if (shareTextToConfirm != null) {
        AlertDialog(
            onDismissRequest = { shareTextToConfirm = null },
            title = {
                Text(
                    text = "Workouts Clubbed!",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Workouts logged within 2 hours have been clubbed together. You can edit them in the sharing app.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "My HYROX Workout Results")
                            putExtra(android.content.Intent.EXTRA_TEXT, shareTextToConfirm)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share Workout Result via"))
                        shareTextToConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("SHARE", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { shareTextToConfirm = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (editingResult != null) {
        val res = editingResult!!
        val dialogWorkout = state.workouts.find { it.id == res.workoutId }
        
        if (dialogWorkout != null) {
            val editInputs = remember(res) { mutableStateMapOf<MeasureType, String>() }
            val editCheckmarks = remember(res) { mutableStateMapOf<MeasureType, Boolean>() }
            var editNotes by remember(res) { mutableStateOf(res.notes) }
            val editWeightUnits = remember(res) { mutableStateMapOf<MeasureType, String>() }
            var editError by remember(res) { mutableStateOf<String?>(null) }

            // Pre-fill values
            remember(res) {
                res.values.forEach { valObj ->
                    if (valObj.measureType == MeasureType.CHECKMARK) {
                        editCheckmarks[valObj.measureType] = valObj.doubleValue == 1.0
                    } else if (valObj.measureType == MeasureType.WEIGHT || valObj.measureType == MeasureType.WEIGHTLIFTING) {
                        val raw = valObj.stringValue
                        val unit = if (raw.endsWith("lbs")) "lbs" else "kg"
                        val valOnly = raw.removeSuffix("lbs").removeSuffix("kg").trim()
                        editInputs[valObj.measureType] = valOnly
                        editWeightUnits[valObj.measureType] = unit
                    } else {
                        editInputs[valObj.measureType] = valObj.stringValue
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { editingResult = null },
                title = {
                    Text(
                        text = "Edit Log Entry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        dialogWorkout.measures.forEach { measure ->
                            when (measure) {
                                MeasureType.CHECKMARK -> {
                                    val isChecked = editCheckmarks[measure] ?: false
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editCheckmarks[measure] = !isChecked }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { editCheckmarks[measure] = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Mark Exercise as Completed",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                else -> {
                                    val isWeight = measure == MeasureType.WEIGHT || measure == MeasureType.WEIGHTLIFTING
                                    if (isWeight) {
                                        val currentUnit = editWeightUnits[measure] ?: "kg"
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Weight Unit:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (currentUnit == "kg") MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    .clickable {
                                                        if (currentUnit == "lbs") {
                                                            // Convert lbs to kg
                                                            val currentText = editInputs[measure] ?: ""
                                                            if (currentText.isNotBlank()) {
                                                                if (measure == MeasureType.WEIGHT) {
                                                                    val valD = currentText.toDoubleOrNull()
                                                                    if (valD != null) {
                                                                        editInputs[measure] = String.format("%.1f", valD / 2.20462).removeSuffix(".0").replace(",", ".")
                                                                    }
                                                                } else {
                                                                    val wPart = currentText.substringBefore("x").trim()
                                                                    val rPart = currentText.substringAfter("x", "").trim()
                                                                    val valD = wPart.toDoubleOrNull()
                                                                    if (valD != null) {
                                                                        val conv = String.format("%.1f", valD / 2.20462).removeSuffix(".0").replace(",", ".")
                                                                        editInputs[measure] = if (rPart.isNotEmpty()) "$conv x $rPart" else conv
                                                                    }
                                                                }
                                                            }
                                                            editWeightUnits[measure] = "kg"
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        "KG",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.sp,
                                                        color = if (currentUnit == "kg") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (currentUnit == "lbs") MaterialTheme.colorScheme.primary else Color.Transparent)
                                                    .clickable {
                                                        if (currentUnit == "kg") {
                                                            // Convert kg to lbs
                                                            val currentText = editInputs[measure] ?: ""
                                                            if (currentText.isNotBlank()) {
                                                                if (measure == MeasureType.WEIGHT) {
                                                                    val valD = currentText.toDoubleOrNull()
                                                                    if (valD != null) {
                                                                        editInputs[measure] = String.format("%.1f", valD * 2.20462).removeSuffix(".0").replace(",", ".")
                                                                    }
                                                                } else {
                                                                    val wPart = currentText.substringBefore("x").trim()
                                                                    val rPart = currentText.substringAfter("x", "").trim()
                                                                    val valD = wPart.toDoubleOrNull()
                                                                    if (valD != null) {
                                                                        val conv = String.format("%.1f", valD * 2.20462).removeSuffix(".0").replace(",", ".")
                                                                        editInputs[measure] = if (rPart.isNotEmpty()) "$conv x $rPart" else conv
                                                                    }
                                                                }
                                                            }
                                                            editWeightUnits[measure] = "lbs"
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        "Lbs",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.sp,
                                                        color = if (currentUnit == "lbs") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                val value = editInputs[measure] ?: ""
                                val currentUnit = if (isWeight) (editWeightUnits[measure] ?: "kg") else measure.unit
                                val displaySuffix = if (measure == MeasureType.WEIGHTLIFTING) "$currentUnit x reps" else currentUnit

                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { 
                                        editInputs[measure] = it
                                        editError = null
                                    },
                                    label = { Text(measure.displayName) },
                                    placeholder = { Text(measure.placeholder) },
                                    suffix = {
                                        if (displaySuffix.isNotEmpty()) {
                                            Text(displaySuffix, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { 
                            editNotes = it
                            editError = null
                        },
                        label = { Text("Performance Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    AnimatedVisibility(visible = editError != null) {
                        editError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newValues = mutableListOf<MeasureValue>()
                        var validationFailed = false
                        editError = null

                        // Check if at least one non-checkmark input is filled
                        val nonCheckmarkMeasures = dialogWorkout.measures.filter { it != MeasureType.CHECKMARK }
                        val hasAtLeastOneValue = nonCheckmarkMeasures.any { measure ->
                            val textVal = editInputs[measure] ?: ""
                            textVal.isNotBlank()
                        }

                        if (nonCheckmarkMeasures.isNotEmpty() && !hasAtLeastOneValue) {
                            editError = "Please fill in at least one measurement to save this result."
                            validationFailed = true
                        }

                        if (!validationFailed) {
                            for (measure in dialogWorkout.measures) {
                                val originalVal = res.values.find { it.measureType == measure }
                                val mId = originalVal?.id ?: UUID.randomUUID().toString()

                                if (measure == MeasureType.CHECKMARK) {
                                    val completed = editCheckmarks[measure] ?: false
                                    newValues.add(
                                        MeasureValue(
                                            id = mId,
                                            resultId = res.id,
                                            measureType = measure,
                                            doubleValue = if (completed) 1.0 else 0.0,
                                            stringValue = if (completed) "Completed" else "Not Completed"
                                        )
                                    )
                                } else {
                                    val textVal = editInputs[measure] ?: ""
                                    // Skip saving if the value is blank (optional fields)
                                    if (textVal.isBlank()) {
                                        continue
                                    }

                                    if (measure == MeasureType.TIME) {
                                        val parsedTime = MeasureType.validateAndFormatTime(textVal)
                                        if (parsedTime == null) {
                                            editError = "Invalid time format! Use MM.SS (e.g. 4.16) or an integer (e.g. 12)"
                                            validationFailed = true
                                            break
                                        }
                                        newValues.add(
                                            MeasureValue(
                                                id = mId,
                                                resultId = res.id,
                                                measureType = measure,
                                                doubleValue = parsedTime.first,
                                                stringValue = parsedTime.second
                                            )
                                        )
                                    } else {
                                        // Numeric measures check
                                        if (MeasureType.isNumericMeasure(measure)) {
                                            val dVal = textVal.toDoubleOrNull()
                                            if (dVal == null) {
                                                editError = "${measure.displayName} must be a number (e.g., 10, 10.50). English letters are not allowed!"
                                                validationFailed = true
                                                break
                                            }
                                        }

                                        val isWeight = measure == MeasureType.WEIGHT || measure == MeasureType.WEIGHTLIFTING
                                        val unitSuffix = if (isWeight) (editWeightUnits[measure] ?: "kg") else ""

                                        val dVal = if (measure == MeasureType.WEIGHTLIFTING) {
                                            textVal.substringBefore("x").trim().toDoubleOrNull() ?: 0.0
                                        } else {
                                            textVal.toDoubleOrNull() ?: 0.0
                                        }

                                        val displayString = if (isWeight) "$textVal $unitSuffix" else textVal

                                        newValues.add(
                                            MeasureValue(
                                                id = mId,
                                                resultId = res.id,
                                                measureType = measure,
                                                doubleValue = dVal,
                                                stringValue = displayString
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (!validationFailed) {
                            viewModel.updateWorkoutResult(res.id, res.workoutId, res.timestamp, newValues, editNotes)
                            editingResult = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("SAVE CHANGES")
                }
            },
            dismissButton = {
                Button(
                    onClick = { editingResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}
}
