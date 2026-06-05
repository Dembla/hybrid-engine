package com.example.hyroxtraining.ui.settings

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.hyroxtraining.ui.auth.AuthViewModel
import com.example.hyroxtraining.ui.main.MainScreenViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    mainViewModel: MainScreenViewModel,
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.state.collectAsState()
    val mainState by mainViewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedWorkoutId by remember { mutableStateOf<String?>(null) }
    var showWorkoutSelector by remember { mutableStateOf(false) }
    var editingResult by remember { mutableStateOf<WorkoutResult?>(null) }
    var showDeleteAccountConfirmDialog by remember { mutableStateOf(false) }

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
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Athlete Profile Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = authState.username.ifEmpty { "Athlete" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = authState.email.ifEmpty { "athlete@hyrox.com" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings items
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Theme Toggle Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dark Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Switch between light and dark UI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { onThemeToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                // Accessible Font Size Item
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Accessible Font Size",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Make app text larger and easier to read",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val sizes = listOf(
                            Triple("STANDARD", 1.0f, "1.0x"),
                            Triple("MEDIUM", 1.15f, "1.15x"),
                            Triple("LARGE", 1.30f, "1.3x")
                        )

                        sizes.forEach { (name, scale, label) ->
                            val isSelected = fontScale == scale
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .clickable { onFontScaleChange(scale) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // EDIT WORKOUT HISTORY LOGS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EDIT PAST LOGS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Workout Selector Button
                val selectedWorkout = mainState.workouts.find { it.id == selectedWorkoutId }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWorkoutSelector = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedWorkout?.name ?: "Select Exercise to Edit",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedWorkout != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "CHANGE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (selectedWorkout != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val logs = mainState.results
                        .filter { it.workoutId == selectedWorkoutId }
                        .sortedByDescending { it.timestamp }
                    
                    if (logs.isEmpty()) {
                        Text(
                            text = "No recorded logs found for this exercise.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        logs.forEach { log ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = DateFormat.format("EEE, MMM dd, yyyy - h:mm a", Date(log.timestamp)).toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row {
                                            IconButton(
                                                onClick = { editingResult = log },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Log",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { mainViewModel.deleteResult(log.id) },
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    log.values.forEach { valObj ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
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
                                    if (log.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Notes: ${log.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Log out button
        Button(
            onClick = {
                mainViewModel.clearLocalData()
                authViewModel.logout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary // high contrast
            )
        ) {
            Text(
                text = "SIGN OUT ATHLETE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Delete Account button
        Button(
            onClick = { showDeleteAccountConfirmDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(
                text = "DELETE MY ACCOUNT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    // Dialog for Workout Selection in Settings
    if (showWorkoutSelector) {
        AlertDialog(
            onDismissRequest = { showWorkoutSelector = false },
            title = { Text("Select Exercise to Edit", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    mainState.workouts.forEach { w ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedWorkoutId = w.id
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

    // Dialog for Editing Log entry inside Settings
    if (editingResult != null) {
        val res = editingResult!!
        val dialogWorkout = mainState.workouts.find { it.id == res.workoutId }
        
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
                                }
                            }

                            val value = editInputs[measure] ?: ""
                            val currentUnit = if (measure == MeasureType.WEIGHT || measure == MeasureType.WEIGHTLIFTING) (editWeightUnits[measure] ?: "kg") else measure.unit
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
                                mainViewModel.updateWorkoutResult(res.id, res.workoutId, res.timestamp, newValues, editNotes)
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

    // Dialog for Delete Account Confirmation
    if (showDeleteAccountConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirmDialog = false },
            title = { Text("Delete Your Athlete Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete your account? This action is NOT immediately reversible.\n\nYour account will be put into a dormant state for 90 days. During this period, you can log back in at any time to cancel the deletion and fully restore your training data. After 90 days, all your data will be permanently and completely deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirmDialog = false
                        // Get current profile
                        val profile = mainState.userProfile ?: com.example.hyroxtraining.data.UserProfile(
                            userId = authState.email.substringBefore("@"),
                            firstName = "",
                            lastName = "",
                            dateOfBirth = "",
                            country = "",
                            state = "",
                            city = "",
                            pincode = ""
                        )
                        // Trigger soft delete deletion scheduled
                        authViewModel.scheduleAccountDeletion(profile)
                        mainViewModel.deleteUserDataLocally()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) {
                    Text("DELETE ACCOUNT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteAccountConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}
