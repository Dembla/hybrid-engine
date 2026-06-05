package com.example.hyroxtraining.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.hyroxtraining.data.DefaultDataRepository
import com.example.hyroxtraining.ui.auth.AuthViewModel
import com.example.hyroxtraining.ui.auth.LoginScreen
import com.example.hyroxtraining.ui.dashboard.DashboardScreen
import com.example.hyroxtraining.ui.results.ResultsScreen
import com.example.hyroxtraining.ui.schedule.ScheduleScreen
import com.example.hyroxtraining.ui.settings.SettingsScreen
import com.example.hyroxtraining.ui.workout.WorkoutDetailScreen
import com.example.hyroxtraining.ui.workout.WorkoutListScreen

sealed class Screen {
    data class Tab(val index: Int) : Screen()
    data class WorkoutDetail(val workoutId: String) : Screen()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mainViewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository(context)) }
    val authState by authViewModel.state.collectAsState()

    // 1. Force Login Screen first if not authenticated
    if (!authState.isLoggedIn) {
        LoginScreen(
            viewModel = authViewModel,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // 1.5. Intercept and show Dormant Account Recovery Dialog if user account is scheduled for deletion
    if (authState.dormantProfileToRestore != null) {
        val dormantProfile = authState.dormantProfileToRestore!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { authViewModel.logout() },
            title = { Text("Restore Scheduled Deletion Account?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Welcome back, ${dormantProfile.firstName}!\n\nYour account is currently scheduled for deletion. All of your training history, results, and weekly schedule items are still preserved.\n\nWould you like to cancel the deletion schedule and fully restore your athlete profile?"
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        authViewModel.cancelAccountDeletion(context, dormantProfile)
                        mainViewModel.forceSync()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("RESTORE ACCOUNT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.Button(
                    onClick = { authViewModel.logout() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("LOG OUT")
                }
            }
        )
        return
    }

    val mainState by mainViewModel.uiState.collectAsState()

    // 1.7. Force Onboarding screen if profile is not completed
    if (mainState.hasLoadedProfile && mainState.userProfile == null) {
        com.example.hyroxtraining.ui.auth.OnboardingScreen(
            viewModel = mainViewModel,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // 2. Queue-based Backstack Navigation Shell
    val navHistory = remember { mutableStateListOf<Screen>(Screen.Tab(0)) }

    // Bind system/hardware back press to navigation backstack
    BackHandler(enabled = navHistory.size > 1) {
        navHistory.removeAt(navHistory.lastIndex)
    }

    val currentTab = navHistory.lastOrNull { it is Screen.Tab } as? Screen.Tab
    val selectedTab = currentTab?.index ?: 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Home Tab
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        if (navHistory.lastOrNull() != Screen.Tab(0)) {
                            navHistory.add(Screen.Tab(0))
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp)) },
                    label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Schedule Tab
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        if (navHistory.lastOrNull() != Screen.Tab(1)) {
                            navHistory.add(Screen.Tab(1))
                        }
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule", modifier = Modifier.size(22.dp)) },
                    label = { Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Workout Tab
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        if (navHistory.lastOrNull() != Screen.Tab(2)) {
                            navHistory.add(Screen.Tab(2))
                        }
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Workout", modifier = Modifier.size(22.dp)) },
                    label = { Text("Workout", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Results Tab
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        if (navHistory.lastOrNull() != Screen.Tab(3)) {
                            navHistory.add(Screen.Tab(3))
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Results", modifier = Modifier.size(22.dp)) },
                    label = { Text("Results", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Settings Tab
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        if (navHistory.lastOrNull() != Screen.Tab(4)) {
                            navHistory.add(Screen.Tab(4))
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp)) },
                    label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            val activeScreen = navHistory.lastOrNull() ?: Screen.Tab(0)
            AnimatedContent(
                targetState = activeScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { screen ->
                when (screen) {
                    is Screen.WorkoutDetail -> {
                        WorkoutDetailScreen(
                            workoutId = screen.workoutId,
                            viewModel = mainViewModel,
                            onBack = {
                                if (navHistory.size > 1) {
                                    navHistory.removeAt(navHistory.lastIndex)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is Screen.Tab -> {
                        when (screen.index) {
                            0 -> DashboardScreen(
                                authViewModel = authViewModel,
                                mainViewModel = mainViewModel,
                                onNavigateToWorkouts = {
                                    if (navHistory.lastOrNull() != Screen.Tab(2)) {
                                        navHistory.add(Screen.Tab(2))
                                    }
                                },
                                onWorkoutSelect = { workoutId ->
                                    navHistory.add(Screen.WorkoutDetail(workoutId))
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            1 -> ScheduleScreen(
                                viewModel = mainViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            2 -> WorkoutListScreen(
                                viewModel = mainViewModel,
                                onWorkoutSelect = { workoutId ->
                                    navHistory.add(Screen.WorkoutDetail(workoutId))
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            3 -> ResultsScreen(
                                viewModel = mainViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            4 -> SettingsScreen(
                                authViewModel = authViewModel,
                                mainViewModel = mainViewModel,
                                isDark = isDark,
                                onThemeToggle = onThemeToggle,
                                fontScale = fontScale,
                                onFontScaleChange = onFontScaleChange,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
