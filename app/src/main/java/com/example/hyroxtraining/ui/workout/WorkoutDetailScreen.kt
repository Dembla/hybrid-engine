package com.example.hyroxtraining.ui.workout

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hyroxtraining.data.MeasureType
import com.example.hyroxtraining.data.MeasureValue
import com.example.hyroxtraining.data.Workout
import com.example.hyroxtraining.data.WorkoutResult
import com.example.hyroxtraining.ui.main.MainScreenViewModel
import java.util.Date
import java.util.UUID

@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    viewModel: MainScreenViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val workout = state.workouts.find { it.id == workoutId } ?: return

    val historicalResults = state.results.filter { it.workoutId == workoutId }
    val inputs = viewModel.workoutDraftInputs[workoutId] ?: emptyMap()
    val checkmarkInputs = viewModel.workoutDraftCheckmarks[workoutId] ?: emptyMap()
    val weightUnits = viewModel.workoutDraftWeightUnits[workoutId] ?: emptyMap()
    var editingResult by remember { mutableStateOf<WorkoutResult?>(null) }
    var shareTextToConfirm by remember { mutableStateOf<String?>(null) }
    val notes = viewModel.workoutDraftNotes[workoutId] ?: ""
    var logError by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    fun getFormattedValue(valObj: MeasureValue): String {
        return if (valObj.measureType == MeasureType.WEIGHT || valObj.measureType == MeasureType.WEIGHTLIFTING) {
            valObj.stringValue
        } else {
            "${valObj.stringValue} ${valObj.measureType.unit}".trim()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Button & Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = workout.name.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Workout",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Description",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = workout.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Entry Logger Panel
        Text(
            text = "LOG PERFORMANCE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                workout.measures.forEach { measure ->
                    when (measure) {
                        MeasureType.CHECKMARK -> {
                            val isChecked = checkmarkInputs[measure] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.workoutDraftCheckmarks[workoutId] = checkmarkInputs + (measure to !isChecked)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        viewModel.workoutDraftCheckmarks[workoutId] = checkmarkInputs + (measure to it)
                                    },
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
                            val value = inputs[measure] ?: ""
                            val isWeightMeasure = measure == MeasureType.WEIGHT || measure == MeasureType.WEIGHTLIFTING
                            if (isWeightMeasure) {
                                val currentUnit = weightUnits[measure] ?: "kg"
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
                                                    viewModel.workoutDraftWeightUnits[workoutId] = weightUnits + (measure to "kg")
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
                                                    viewModel.workoutDraftWeightUnits[workoutId] = weightUnits + (measure to "lbs")
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

                            val currentUnit = if (isWeightMeasure) (weightUnits[measure] ?: "kg") else measure.unit
                            val displaySuffix = if (measure == MeasureType.WEIGHTLIFTING) "$currentUnit x reps" else currentUnit

                            OutlinedTextField(
                                value = value,
                                onValueChange = {
                                    viewModel.workoutDraftInputs[workoutId] = inputs + (measure to it)
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

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        viewModel.workoutDraftNotes[workoutId] = it
                    },
                    label = { Text("Performance Notes") },
                    placeholder = { Text("e.g. Felt strong today, transition was quick") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Error indicator
                AnimatedVisibility(visible = logError != null) {
                    logError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit button
                Button(
                    onClick = {
                        val newValues = mutableListOf<MeasureValue>()
                        var validationFailed = false
                        logError = null

                        // Check if at least one non-checkmark input is filled
                        val nonCheckmarkMeasures = workout.measures.filter { it != MeasureType.CHECKMARK }
                        val hasAtLeastOneValue = nonCheckmarkMeasures.any { measure ->
                            val textVal = inputs[measure] ?: ""
                            textVal.isNotBlank()
                        }

                        if (nonCheckmarkMeasures.isNotEmpty() && !hasAtLeastOneValue) {
                            logError = "Please fill in at least one measurement to save this result."
                            validationFailed = true
                        }

                        if (!validationFailed) {
                            for (measure in workout.measures) {
                                if (measure == MeasureType.CHECKMARK) {
                                    val completed = checkmarkInputs[measure] ?: false
                                    newValues.add(
                                        MeasureValue(
                                            id = UUID.randomUUID().toString(),
                                            resultId = "",
                                            measureType = measure,
                                            doubleValue = if (completed) 1.0 else 0.0,
                                            stringValue = if (completed) "Completed" else "Not Completed"
                                        )
                                    )
                                } else {
                                    val textVal = inputs[measure] ?: ""
                                    // Skip saving if the value is blank (optional fields)
                                    if (textVal.isBlank()) {
                                        continue
                                    }

                                    if (measure == MeasureType.TIME) {
                                        val parsedTime = MeasureType.validateAndFormatTime(textVal)
                                        if (parsedTime == null) {
                                            logError = "Invalid time format! Use MM.SS (e.g. 4.16) or an integer (e.g. 12)"
                                            validationFailed = true
                                            break
                                        }
                                        newValues.add(
                                            MeasureValue(
                                                id = UUID.randomUUID().toString(),
                                                resultId = "",
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
                                                logError = "${measure.displayName} must be a number (e.g., 10, 10.50). English letters are not allowed!"
                                                validationFailed = true
                                                break
                                            }
                                        }

                                        val isWeight = measure == MeasureType.WEIGHT || measure == MeasureType.WEIGHTLIFTING
                                        val unitSuffix = if (isWeight) (weightUnits[measure] ?: "kg") else ""

                                        val dVal = if (measure == MeasureType.WEIGHTLIFTING) {
                                            textVal.substringBefore("x").trim().toDoubleOrNull() ?: 0.0
                                        } else {
                                            textVal.toDoubleOrNull() ?: 0.0
                                        }

                                        val displayString = if (isWeight) "$textVal $unitSuffix" else textVal

                                        newValues.add(
                                            MeasureValue(
                                                id = UUID.randomUUID().toString(),
                                                resultId = "",
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
                            viewModel.logWorkoutResult(workout.id, newValues, notes)
                            // Reset inputs
                            viewModel.workoutDraftInputs.remove(workout.id)
                            viewModel.workoutDraftCheckmarks.remove(workout.id)
                            viewModel.workoutDraftWeightUnits.remove(workout.id)
                            viewModel.workoutDraftNotes.remove(workout.id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("SAVE TO HISTORY", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History Log Comparison List
        Text(
            text = "PREVIOUS PERFORMANCES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (historicalResults.isEmpty()) {
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
                        text = "No history available. First logged result will set your Benchmark record!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            historicalResults.forEachIndexed { index, result ->
                val dateString = DateFormat.format("MMM dd, yyyy - hh:mm a", Date(result.timestamp)).toString()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateString.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.weight(1f)
                            )
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
                                    onClick = { viewModel.deleteResult(result.id) },
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

                        if (index == 0) {
                            // Highlight "Latest Log"
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
                                    "MOST RECENT",
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

    if (showEditDialog) {
        var name by remember(workout) { mutableStateOf(workout.name) }
        var description by remember(workout) { mutableStateOf(workout.description) }
        val selectedMeasures = remember(workout) {
            mutableStateListOf<MeasureType>().apply {
                addAll(workout.measures)
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    "Edit Custom Workout",
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Workout Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Workout Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SELECT MEASURES (UP TO 3)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MeasureType.values().forEach { measureType ->
                        val isChecked = selectedMeasures.contains(measureType)
                        val isLimitReached = selectedMeasures.size >= 3

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isChecked || !isLimitReached) {
                                    if (isChecked) {
                                        selectedMeasures.remove(measureType)
                                    } else {
                                        selectedMeasures.add(measureType)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!isLimitReached) selectedMeasures.add(measureType)
                                    } else {
                                        selectedMeasures.remove(measureType)
                                    }
                                },
                                enabled = isChecked || !isLimitReached,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = measureType.displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked || !isLimitReached) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && selectedMeasures.isNotEmpty()) {
                            viewModel.updateCustomWorkout(workout.id, name, description, selectedMeasures.toList())
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    enabled = name.isNotBlank() && selectedMeasures.isNotEmpty()
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("CANCEL")
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
                    workout.measures.forEach { measure ->
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
                        val nonCheckmarkMeasures = workout.measures.filter { it != MeasureType.CHECKMARK }
                        val hasAtLeastOneValue = nonCheckmarkMeasures.any { measure ->
                            val textVal = editInputs[measure] ?: ""
                            textVal.isNotBlank()
                        }

                        if (nonCheckmarkMeasures.isNotEmpty() && !hasAtLeastOneValue) {
                            editError = "Please fill in at least one measurement to save this result."
                            validationFailed = true
                        }

                        if (!validationFailed) {
                            for (measure in workout.measures) {
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
