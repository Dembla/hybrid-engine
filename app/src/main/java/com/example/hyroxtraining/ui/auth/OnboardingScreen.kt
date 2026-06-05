package com.example.hyroxtraining.ui.auth

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hyroxtraining.ui.main.MainScreenViewModel

@Composable
fun OnboardingScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }

    // State for Step 1
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") } // YYYY-MM-DD
    var step1Error by remember { mutableStateOf<String?>(null) }

    // State for Step 2
    var country by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var step2Error by remember { mutableStateOf<String?>(null) }

    var showCountryDialog by remember { mutableStateOf(false) }
    var showStateDialog by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }

    val countries = listOf(
        "Canada", "United States", "India", "United Kingdom", "Australia",
        // European Union Countries & major European nations
        "Austria", "Belgium", "Bulgaria", "Croatia", "Cyprus", "Czech Republic", "Denmark",
        "Estonia", "Finland", "France", "Germany", "Greece", "Hungary", "Ireland", "Italy",
        "Latvia", "Lithuania", "Luxembourg", "Malta", "Netherlands", "Poland", "Portugal",
        "Romania", "Slovakia", "Slovenia", "Spain", "Sweden", "Switzerland", "Norway", "Iceland",
        "Ukraine", "New Zealand", "Singapore", "Japan", "South Africa", "Brazil", "Mexico",
        "United Arab Emirates", "Saudi Arabia", "Argentina", "Turkey", "South Korea", "China",
        "Egypt", "Nigeria", "Kenya", "Vietnam", "Thailand"
    ).sorted()

    val statesMap = mapOf(
        "Canada" to listOf(
            "ON (Ontario)", "QC (Quebec)", "BC (British Columbia)", "AB (Alberta)", "MB (Manitoba)",
            "SK (Saskatchewan)", "NS (Nova Scotia)", "NB (New Brunswick)", "NL (Newfoundland & Labrador)",
            "PE (Prince Edward Island)", "YT (Yukon)", "NT (Northwest Territories)", "NU (Nunavut)"
        ),
        "United States" to listOf(
            "AL (Alabama)", "AK (Alaska)", "AZ (Arizona)", "AR (Arkansas)", "CA (California)",
            "CO (Colorado)", "CT (Connecticut)", "DE (Delaware)", "FL (Florida)", "GA (Georgia)",
            "HI (Hawaii)", "ID (Idaho)", "IL (Illinois)", "IN (Indiana)", "IA (Iowa)", "KS (Kansas)",
            "KY (Kentucky)", "LA (Louisiana)", "ME (Maine)", "MD (Maryland)", "MA (Massachusetts)",
            "MI (Michigan)", "MN (Minnesota)", "MS (Mississippi)", "MO (Missouri)", "MT (Montana)",
            "NE (Nebraska)", "NV (Nevada)", "NH (New Hampshire)", "NJ (New Jersey)", "NM (New Mexico)",
            "NY (New York)", "NC (North Carolina)", "ND (North Dakota)", "OH (Ohio)", "OK (Oklahoma)",
            "OR (Oregon)", "PA (Pennsylvania)", "RI (Rhode Island)", "SC (South Carolina)",
            "SD (South Dakota)", "TN (Tennessee)", "TX (Texas)", "UT (Utah)", "VT (Vermont)",
            "VA (Virginia)", "WA (Washington)", "WV (West Virginia)", "WI (Wisconsin)", "WY (Wyoming)"
        ),
        "India" to listOf(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat",
            "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
            "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand",
            "West Bengal", "Andaman and Nicobar", "Chandigarh", "Dadra and Nagar Haveli & Daman and Diu",
            "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
        ),
        "United Kingdom" to listOf("England", "Scotland", "Wales", "Northern Ireland"),
        "Australia" to listOf(
            "NSW (New South Wales)", "VIC (Victoria)", "QLD (Queensland)", "WA (Western Australia)",
            "SA (South Australia)", "TAS (Tasmania)", "ACT (Australian Capital Territory)", "NT (Northern Territory)"
        )
    )

    // Dynamic Validation Checkers
    fun getNormalizedCountry(c: String): String {
        val trimmed = c.trim().lowercase()
        return when {
            trimmed == "canada" || trimmed.startsWith("can") -> "Canada"
            trimmed == "united states" || trimmed == "usa" || trimmed == "us" || trimmed == "united states of america" -> "United States"
            trimmed == "india" || trimmed.startsWith("ind") -> "India"
            trimmed == "united kingdom" || trimmed == "uk" || trimmed == "great britain" || trimmed == "england" -> "United Kingdom"
            trimmed == "australia" || trimmed.startsWith("aus") -> "Australia"
            else -> c.trim()
        }
    }

    fun getPincodeKeyboardType(country: String): KeyboardType {
        val norm = getNormalizedCountry(country)
        return when (norm) {
            "United States", "India", "Australia" -> KeyboardType.Number
            else -> KeyboardType.Text
        }
    }

    fun validatePincodeFormat(pincode: String, country: String): Boolean {
        val clean = pincode.trim()
        if (clean.isEmpty()) return false
        val norm = getNormalizedCountry(country)
        return when (norm) {
            "Canada" -> clean.length <= 6 && clean.all { it.isLetterOrDigit() }
            "United States" -> (clean.length == 5 || clean.length == 9) && clean.all { it.isDigit() }
            "India" -> clean.length == 6 && clean.all { it.isDigit() }
            "United Kingdom" -> clean.length <= 8
            "Australia" -> clean.length == 4 && clean.all { it.isDigit() }
            else -> clean.length in 3..10 && clean.all { it.isLetterOrDigit() }
        }
    }

    fun getPincodeHelperText(country: String): String {
        val norm = getNormalizedCountry(country)
        return when (norm) {
            "Canada" -> "Alphanumeric, max 6 chars (e.g. K1A0B1)"
            "United States" -> "Numbers only, 5 or 9 digits"
            "India" -> "Numbers only, exactly 6 digits"
            "United Kingdom" -> "Alphanumeric, max 8 chars"
            "Australia" -> "Numbers only, exactly 4 digits"
            else -> "Alphanumeric, 3 to 10 characters"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sporty Badge Header
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "COMPLETE YOUR PROFILE",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "STEP $step OF 3",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Glass Card Wrapper
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    
                    if (step == 1) {
                        // STEP 1: PERSONAL DETAILS
                        Text(
                            text = "Profile Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Please complete your general athlete profile details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { 
                                firstName = it
                                step1Error = null
                            },
                            label = { Text("First Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { 
                                lastName = it
                                step1Error = null
                            },
                            label = { Text("Last Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = dob,
                            onValueChange = { input ->
                                val cleanInput = input.filter { it.isDigit() }.take(8)
                                dob = cleanInput
                                step1Error = null
                            },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            placeholder = { Text("e.g. 1995-10-24") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = DateVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        AnimatedVisibility(visible = step1Error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = step1Error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val dobRegex = Regex("""^\d{8}$""")
                                when {
                                    firstName.trim().isEmpty() -> step1Error = "First Name cannot be blank!"
                                    lastName.trim().isEmpty() -> step1Error = "Last Name cannot be blank!"
                                    dob.trim().isEmpty() -> step1Error = "Date of Birth is required!"
                                    !dobRegex.matches(dob) -> step1Error = "Date of Birth must be 8 digits (YYYYMMDD)!"
                                    else -> {
                                        val year = dob.substring(0, 4).toIntOrNull() ?: 0
                                        val month = dob.substring(4, 6).toIntOrNull() ?: 0
                                        val day = dob.substring(6, 8).toIntOrNull() ?: 0
                                        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                        
                                        if (year !in 1900..currentYear) {
                                            step1Error = "Year must be between 1900 and $currentYear!"
                                        } else if (month !in 1..12) {
                                            step1Error = "Month must be between 01 and 12!"
                                        } else {
                                            val maxDays = when (month) {
                                                2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
                                                4, 6, 9, 11 -> 30
                                                else -> 31
                                            }
                                            if (day !in 1..maxDays) {
                                                step1Error = "Day must be between 01 and $maxDays for month ${month.toString().padStart(2, '0')}!"
                                            } else {
                                                step1Error = null
                                                step = 2
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("NEXT", fontWeight = FontWeight.ExtraBold)
                        }
                    } else if (step == 2) {
                        // STEP 2: LOCATION DETAILS
                        Text(
                            text = "Athlete Location",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Calibrate training zone and local training metrics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Custom Country Selector Trigger
                        OutlinedTextField(
                            value = country,
                            onValueChange = { 
                                country = it
                                state = "" // reset state when country changes
                                step2Error = null
                            },
                            label = { Text("Country") },
                            placeholder = { Text("e.g. India, United States, Germany") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showCountryDialog = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select from List")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        val normalizedCountry = getNormalizedCountry(country)
                        if (normalizedCountry.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            if (statesMap.containsKey(normalizedCountry)) {
                                // Custom State Selector Trigger
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showStateDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = state.ifEmpty { "Select State/Province" },
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (state.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = state,
                                    onValueChange = { 
                                        state = it
                                        step2Error = null
                                    },
                                    label = { Text("State / Province / Region") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = city,
                            onValueChange = { 
                                city = it
                                step2Error = null
                            },
                            label = { Text("City") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        if (country.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = pincode,
                                onValueChange = { input ->
                                    // Smart capitalization & filtering depending on country selection
                                    val cleaned = if (getPincodeKeyboardType(country) == KeyboardType.Number) {
                                        input.filter { it.isDigit() }
                                    } else {
                                        input.uppercase()
                                    }
                                    pincode = cleaned
                                    step2Error = null
                                },
                                label = { Text("Pincode / Postal Code") },
                                supportingText = { Text(getPincodeHelperText(country)) },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = getPincodeKeyboardType(country)),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        AnimatedVisibility(visible = step2Error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = step2Error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("BACK", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    when {
                                        country.isEmpty() -> step2Error = "Please select a Country!"
                                        city.trim().isEmpty() -> step2Error = "City is required!"
                                        pincode.trim().isNotEmpty() && !validatePincodeFormat(pincode, country) -> {
                                            step2Error = "Invalid Pincode/ZIP format for ${country}!\nFormat: ${getPincodeHelperText(country)}"
                                        }
                                        else -> {
                                            step2Error = null
                                            step = 3
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("NEXT", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    } else if (step == 3) {
                        // STEP 3: PRIVACY NOTICE & AGREEMENT
                        Text(
                            text = "Privacy & Consent",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Personally Identifiable Info (PII) Notice",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "To customize cardiovascular zones, track local performance metrics, and synchronize backups, this app collects First Name, Last Name, Birth Date, and Location.\n\nYour data is locally isolated in encrypted local storage and synced over secure TLS to Firebase. No data is ever shared with third parties. You may delete your account and clear all saved records at any time inside app Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { step = 2 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("BACK", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val formattedDob = if (dob.length == 8) {
                                        "${dob.substring(0, 4)}-${dob.substring(4, 6)}-${dob.substring(6, 8)}"
                                    } else {
                                        dob
                                    }
                                    viewModel.saveUserProfile(
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        dob = formattedDob,
                                        country = country,
                                        state = state,
                                        city = city.trim(),
                                        pincode = pincode.trim()
                                    )
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("AGREE & SAVE", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Choice Picker Dialogs
    if (showCountryDialog) {
        val filteredCountries = countries.filter { it.contains(countrySearchQuery, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { 
                showCountryDialog = false
                countrySearchQuery = ""
            },
            title = { Text("Select Country", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = countrySearchQuery,
                        onValueChange = { countrySearchQuery = it },
                        label = { Text("Search Country") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Column(modifier = Modifier.fillMaxWidth().height(250.dp).verticalScroll(rememberScrollState())) {
                        if (filteredCountries.isEmpty()) {
                            Text("No countries found", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            filteredCountries.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            country = item
                                            state = ""
                                            pincode = ""
                                            countrySearchQuery = ""
                                            showCountryDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (country == item) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(item, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    val normalizedCountryForState = getNormalizedCountry(country)
    if (showStateDialog && normalizedCountryForState.isNotEmpty()) {
        val statesList = statesMap[normalizedCountryForState] ?: emptyList()
        AlertDialog(
            onDismissRequest = { showStateDialog = false },
            title = { Text("Select State/Province", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState())) {
                    statesList.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    state = item
                                    showStateDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (state == item) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = item,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

class DateVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val trimmed = if (text.text.length > 8) text.text.substring(0, 8) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 3 || i == 5) {
                out += "-"
            }
        }
        val dateOffsetTranslator = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 5) return offset + 1
                return offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 6) return offset - 1
                return offset - 2
            }
        }
        return TransformedText(
            androidx.compose.ui.text.AnnotatedString(out),
            dateOffsetTranslator
        )
    }
}
