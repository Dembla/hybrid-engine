package com.example.hyroxtraining.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hyroxtraining.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

data class AuthState(
    val email: String = "",
    val username: String = "",
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val dormantProfileToRestore: UserProfile? = null
)

class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val emailStr = currentUser.email ?: ""
            // Initially allow session if verified, else sign out
            if (currentUser.isEmailVerified) {
                _state.value = AuthState(
                    email = emailStr,
                    username = emailStr.substringBefore("@").replaceFirstChar { it.uppercase() }.ifEmpty { "Athlete" },
                    isLoggedIn = true,
                    isLoading = true
                )
                val appContext = try {
                    com.google.firebase.FirebaseApp.getInstance().applicationContext
                } catch (e: Exception) {
                    null
                }
                checkDormantStatusOnLogin(appContext, currentUser.uid) { allowed ->
                    _state.value = _state.value.copy(isLoading = false)
                }
            } else {
                FirebaseAuth.getInstance().signOut()
                _state.value = AuthState(isLoggedIn = false)
            }

            // Asynchronously reload to ensure status is up to date
            currentUser.reload().addOnCompleteListener { task ->
                val freshUser = FirebaseAuth.getInstance().currentUser
                if (freshUser != null) {
                    if (freshUser.isEmailVerified) {
                        val freshEmail = freshUser.email ?: ""
                        _state.value = _state.value.copy(
                            email = freshEmail,
                            username = freshEmail.substringBefore("@").replaceFirstChar { it.uppercase() }.ifEmpty { "Athlete" },
                            isLoggedIn = true,
                            isLoading = true
                        )
                        val appContext = try {
                            com.google.firebase.FirebaseApp.getInstance().applicationContext
                        } catch (e: Exception) {
                            null
                        }
                        checkDormantStatusOnLogin(appContext, freshUser.uid) { allowed ->
                            _state.value = _state.value.copy(isLoading = false)
                        }
                    } else {
                        FirebaseAuth.getInstance().signOut()
                        _state.value = AuthState(isLoggedIn = false)
                    }
                }
            }
        }
    }

    fun login(context: android.content.Context, email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Please fill in all fields")
            return
        }
        if (!email.contains("@")) {
            _state.value = _state.value.copy(error = "Invalid email format")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null)
        
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        if (user.isEmailVerified) {
                            val emailStr = user.email ?: email
                            checkDormantStatusOnLogin(context, user.uid) { allowed ->
                                if (allowed) {
                                    _state.value = _state.value.copy(
                                        email = emailStr,
                                        username = emailStr.substringBefore("@").replaceFirstChar { it.uppercase() }.ifEmpty { "Athlete" },
                                        isLoggedIn = true,
                                        isLoading = false
                                    )
                                }
                            }
                        } else {
                            FirebaseAuth.getInstance().signOut()
                            _state.value = _state.value.copy(
                                error = "Your email has not been verified yet. Please check your inbox for the verification link.",
                                isLoading = false
                            )
                        }
                    } else {
                        _state.value = _state.value.copy(
                            error = "Could not retrieve user context.",
                            isLoading = false
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        error = task.exception?.localizedMessage ?: "Login failed",
                        isLoading = false
                    )
                }
            }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Please fill in all fields")
            return
        }
        if (!email.contains("@")) {
            _state.value = _state.value.copy(error = "Invalid email format")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null)
        
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val emailStr = user?.email ?: email
                    if (user != null) {
                        user.sendEmailVerification()
                            .addOnCompleteListener { verifyTask ->
                                FirebaseAuth.getInstance().signOut()
                                if (verifyTask.isSuccessful) {
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        error = "Verification email sent to $emailStr. Please check your inbox and verify your email before logging in."
                                    )
                                } else {
                                    _state.value = _state.value.copy(
                                        error = "Failed to send verification email: ${verifyTask.exception?.localizedMessage}",
                                        isLoading = false
                                    )
                                }
                            }
                    } else {
                        FirebaseAuth.getInstance().signOut()
                        _state.value = _state.value.copy(
                            error = "Could not retrieve user context after registration.",
                            isLoading = false
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        error = task.exception?.localizedMessage ?: "Registration failed",
                        isLoading = false
                    )
                }
            }
    }

    fun signInWithGoogleToken(context: android.content.Context, idToken: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val emailStr = user?.email ?: ""
                    if (user != null) {
                        checkDormantStatusOnLogin(context, user.uid) { allowed ->
                            if (allowed) {
                                _state.value = _state.value.copy(
                                    email = emailStr,
                                    username = user.displayName ?: emailStr.substringBefore("@").replaceFirstChar { it.uppercase() }.ifEmpty { "Athlete" },
                                    isLoggedIn = true,
                                    isLoading = false
                                )
                            }
                        }
                    }
                } else {
                    _state.value = _state.value.copy(
                        error = task.exception?.localizedMessage ?: "Google Sign-In failed",
                        isLoading = false
                    )
                }
            }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        _state.value = AuthState()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter your email address to reset password.")
            return
        }
        if (!email.contains("@")) {
            _state.value = _state.value.copy(error = "Invalid email format")
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null)
        
        FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Password reset email sent to $email. Please check your inbox and follow the link. Please check your spam folder as well."
                    )
                } else {
                    _state.value = _state.value.copy(
                        error = task.exception?.localizedMessage ?: "Failed to send password reset email",
                        isLoading = false
                    )
                }
            }
    }

    fun checkDormantStatusOnLogin(context: android.content.Context?, userId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(userId)
                    .collection("profile").document("main").get().await()
                if (doc.exists()) {
                    val status = doc.getString("status") ?: "ACTIVE"
                    val deletionScheduledAt = doc.getLong("deletionScheduledAt") ?: 0L
                    
                    val profile = UserProfile(
                        userId = userId,
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        dateOfBirth = doc.getString("dateOfBirth") ?: "",
                        country = doc.getString("country") ?: "",
                        state = doc.getString("state") ?: "",
                        city = doc.getString("city") ?: "",
                        pincode = doc.getString("pincode") ?: "",
                        status = status,
                        deletionScheduledAt = deletionScheduledAt
                    )

                    // Insert the user profile locally so the local DB is instantly populated
                    if (context != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val dbHelper = com.example.hyroxtraining.data.DatabaseHelper(context.applicationContext)
                            dbHelper.insertUserProfile(profile)
                        }
                    }

                    if (status == "DORMANT") {
                        val elapsed = System.currentTimeMillis() - deletionScheduledAt
                        val ninetyDays = 90L * 24 * 60 * 60 * 1000L
                        if (elapsed >= ninetyDays) {
                            // Permanent deletion: purge Firestore data & delete FirebaseAuth account
                            db.collection("users").document(userId).delete().await()
                            FirebaseAuth.getInstance().currentUser?.delete()?.await()
                            FirebaseAuth.getInstance().signOut()
                            // Clear local database profile as well since it's permanently deleted
                            if (context != null) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val dbHelper = com.example.hyroxtraining.data.DatabaseHelper(context.applicationContext)
                                    dbHelper.deleteUserDataLocally(userId)
                                }
                            }
                            _state.value = AuthState(
                                isLoggedIn = false,
                                error = "Your account has been permanently deleted after the 90-day retention period."
                            )
                            onComplete(false)
                            return@launch
                        } else {
                            // Account is dormant, within 90 days. Offer restoration.
                            _state.value = _state.value.copy(
                                email = FirebaseAuth.getInstance().currentUser?.email ?: "",
                                username = profile.firstName.ifEmpty { "Athlete" },
                                dormantProfileToRestore = profile,
                                isLoggedIn = true,
                                isLoading = false
                            )
                            onComplete(false)
                            return@launch
                        }
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(true) // Proceed if error/offline
            }
        }
    }

    fun scheduleAccountDeletion(profile: UserProfile) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        val dormantProfile = profile.copy(
            status = "DORMANT",
            deletionScheduledAt = System.currentTimeMillis()
        )
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val profileDoc = mapOf(
                    "userId" to dormantProfile.userId,
                    "firstName" to dormantProfile.firstName,
                    "lastName" to dormantProfile.lastName,
                    "dateOfBirth" to dormantProfile.dateOfBirth,
                    "country" to dormantProfile.country,
                    "state" to dormantProfile.state,
                    "city" to dormantProfile.city,
                    "pincode" to dormantProfile.pincode,
                    "status" to dormantProfile.status,
                    "deletionScheduledAt" to dormantProfile.deletionScheduledAt,
                    "lastUpdated" to System.currentTimeMillis()
                )
                db.collection("users").document(userId)
                    .collection("profile").document("main")
                    .set(profileDoc).await()

                // Sign out locally
                FirebaseAuth.getInstance().signOut()
                _state.value = AuthState(
                    isLoggedIn = false,
                    error = "Your account is now scheduled for deletion. You can log back in within 90 days to restore it."
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to schedule account deletion: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }

    fun cancelAccountDeletion(context: android.content.Context, profile: UserProfile) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        val activeProfile = profile.copy(
            status = "ACTIVE",
            deletionScheduledAt = 0L
        )
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                // Save locally first so the UI instantly updates without onboarding redirection
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val dbHelper = com.example.hyroxtraining.data.DatabaseHelper(context.applicationContext)
                    dbHelper.insertUserProfile(activeProfile)
                }

                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val profileDoc = mapOf(
                    "userId" to activeProfile.userId,
                    "firstName" to activeProfile.firstName,
                    "lastName" to activeProfile.lastName,
                    "dateOfBirth" to activeProfile.dateOfBirth,
                    "country" to activeProfile.country,
                    "state" to activeProfile.state,
                    "city" to activeProfile.city,
                    "pincode" to activeProfile.pincode,
                    "status" to activeProfile.status,
                    "deletionScheduledAt" to activeProfile.deletionScheduledAt,
                    "lastUpdated" to System.currentTimeMillis()
                )
                db.collection("users").document(userId)
                    .collection("profile").document("main")
                    .set(profileDoc).await()

                val emailStr = currentUser.email ?: ""
                _state.value = AuthState(
                    email = emailStr,
                    username = emailStr.substringBefore("@").replaceFirstChar { it.uppercase() }.ifEmpty { "Athlete" },
                    isLoggedIn = true,
                    dormantProfileToRestore = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to restore account: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }
}
