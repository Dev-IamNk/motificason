package com.nk.motificason.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nk.motificason.ui.auth.AuthScreenType
import com.nk.motificason.ui.auth.AuthViewModel
import com.nk.motificason.ui.auth.LoginScreen
import com.nk.motificason.ui.auth.SignUpScreen
import com.nk.motificason.ui.home.HomeScreen
import io.github.jan.supabase.auth.status.SessionStatus

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.nk.motificason.data.model.Habit
import com.nk.motificason.data.model.LockIn
import com.nk.motificason.ui.home.HomeViewModel
import com.nk.motificason.ui.lockin.LockInScreen
import com.nk.motificason.ui.lockin.LockInViewModel
import com.nk.motificason.ui.achievements.AchievementsScreen
import com.nk.motificason.ui.achievements.AchievementsViewModel
import androidx.compose.ui.platform.LocalContext
import com.nk.motificason.ui.notification.NotificationSettingsScreen
import com.nk.motificason.ui.notification.NotificationSettingsViewModel
import com.nk.motificason.notification.NotificationPreferences
import com.nk.motificason.notification.NotificationScheduler
import com.nk.motificason.ui.routine.DailyRoutineScreen
import com.nk.motificason.ui.routine.DailyRoutineViewModel
import com.nk.motificason.ui.share.DailyRoutineShareCardScreen
import com.nk.motificason.ui.share.ShareCardScreen
import com.nk.motificason.ui.streak.StreakScreen
import com.nk.motificason.ui.streak.StreakViewModel

enum class AuthenticatedScreen {
    HOME,
    LOCK_INS,
    HABIT_STREAK,
    ACHIEVEMENTS,
    NOTIFICATION_SETTINGS,
    SHARE_CARD,
    DAILY_ROUTINE,
    DAILY_ROUTINE_SHARE_CARD
}

@Composable
fun MainApp(
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val sessionStatus by viewModel.sessionStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var authenticatedScreen by rememberSaveable { mutableStateOf(AuthenticatedScreen.HOME) }
    var selectedHabit by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.autoSaver()) { mutableStateOf<Habit?>(null) }
    var selectedLockIn by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.autoSaver()) { mutableStateOf<LockIn?>(null) }

    when (sessionStatus) {
        is SessionStatus.Initializing -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is SessionStatus.Authenticated -> {
            val homeViewModel: HomeViewModel = viewModel()
            val appContext = LocalContext.current.applicationContext
            LaunchedEffect(Unit) {
                val prefs = NotificationPreferences(appContext)
                if (!prefs.isInitialized()) {
                    NotificationScheduler.rescheduleAll(appContext)
                }
            }

            when (authenticatedScreen) {
                AuthenticatedScreen.HOME -> {
                    val homeUiState by homeViewModel.uiState.collectAsState()
                    HomeScreen(
                        uiState = homeUiState,
                        userEmail = viewModel.getCurrentUserEmail(),
                        onToggleHabit = { homeViewModel.toggleHabit(it) },
                        onRefresh = { homeViewModel.loadTodayDashboard() },
                        onLogoutClick = {
                            authenticatedScreen = AuthenticatedScreen.HOME
                            viewModel.signOut()
                        },
                        onNavigateToLockIns = {
                            authenticatedScreen = AuthenticatedScreen.LOCK_INS
                        },
                        onNavigateToAchievements = {
                            authenticatedScreen = AuthenticatedScreen.ACHIEVEMENTS
                        },
                        onNavigateToNotificationSettings = {
                            authenticatedScreen = AuthenticatedScreen.NOTIFICATION_SETTINGS
                        },
                        onNavigateToDailyRoutine = {
                            authenticatedScreen = AuthenticatedScreen.DAILY_ROUTINE
                        },
                        modifier = modifier
                    )
                }
                AuthenticatedScreen.LOCK_INS -> {
                    BackHandler {
                        authenticatedScreen = AuthenticatedScreen.HOME
                        homeViewModel.loadTodayDashboard()
                    }
                    val lockInViewModel: LockInViewModel = viewModel()
                    val lockInUiState by lockInViewModel.uiState.collectAsState()

                    LockInScreen(
                        uiState = lockInUiState,
                        onBackClick = {
                            authenticatedScreen = AuthenticatedScreen.HOME
                            homeViewModel.loadTodayDashboard()
                        },
                        onRefresh = { lockInViewModel.loadLockIns() },
                        onOpenPickDialog = { lockInViewModel.openPickLockInDialog() },
                        onClosePickDialog = { lockInViewModel.closePickLockInDialog() },
                        onSelectPreset = { lockInViewModel.pickPreset(it) },
                        onCustomNameChange = { lockInViewModel.onCustomNameChange(it) },
                        onCustomEmojiChange = { lockInViewModel.onCustomEmojiChange(it) },
                        onCreateCustomLockIn = { lockInViewModel.createCustomLockIn() },
                        onOpenAddHabitDialog = { lockInViewModel.openAddHabitDialog(it) },
                        onCloseAddHabitDialog = { lockInViewModel.closeAddHabitDialog() },
                        onHabitTitleChange = { lockInViewModel.onHabitTitleChange(it) },
                        onCreateHabit = { lockInViewModel.createHabit() },
                        onHabitClick = { habit, lockIn ->
                            selectedHabit = habit
                            selectedLockIn = lockIn
                            authenticatedScreen = AuthenticatedScreen.HABIT_STREAK
                        },
                        modifier = modifier
                    )
                }
                AuthenticatedScreen.HABIT_STREAK -> {
                    BackHandler {
                        authenticatedScreen = AuthenticatedScreen.LOCK_INS
                    }
                    val streakViewModel: StreakViewModel = viewModel()
                    val streakUiState by streakViewModel.uiState.collectAsState()
                    val habit = selectedHabit

                    if (habit != null) {
                        LaunchedEffect(habit.id) {
                            streakViewModel.loadStreak(habit)
                        }
                        StreakScreen(
                            habit = habit,
                            lockIn = selectedLockIn,
                            uiState = streakUiState,
                            onBackClick = {
                                authenticatedScreen = AuthenticatedScreen.LOCK_INS
                            },
                            onShareClick = {
                                authenticatedScreen = AuthenticatedScreen.SHARE_CARD
                            },
                            modifier = modifier
                        )
                    } else {
                        authenticatedScreen = AuthenticatedScreen.LOCK_INS
                    }
                }
                AuthenticatedScreen.ACHIEVEMENTS -> {
                    BackHandler {
                        authenticatedScreen = AuthenticatedScreen.HOME
                        homeViewModel.loadTodayDashboard()
                    }
                    val achievementsViewModel: AchievementsViewModel = viewModel()
                    val achievementsUiState by achievementsViewModel.uiState.collectAsState()

                    AchievementsScreen(
                        uiState = achievementsUiState,
                        onBackClick = {
                            authenticatedScreen = AuthenticatedScreen.HOME
                            homeViewModel.loadTodayDashboard()
                        },
                        onRefresh = { achievementsViewModel.loadAchievements() },
                        modifier = modifier
                    )
                }
                AuthenticatedScreen.NOTIFICATION_SETTINGS -> {
                    BackHandler {
                        authenticatedScreen = AuthenticatedScreen.HOME
                        homeViewModel.loadTodayDashboard()
                    }
                    val notificationViewModel: NotificationSettingsViewModel = viewModel()
                    val notificationUiState by notificationViewModel.uiState.collectAsState()
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        notificationViewModel.loadSettings(context)
                    }

                    NotificationSettingsScreen(
                        uiState = notificationUiState,
                        onToneSelected = { notificationViewModel.selectTone(context, it) },
                        onToggleSlot = { slot, enabled -> notificationViewModel.toggleSlot(context, slot, enabled) },
                        onTimeUpdated = { slot, newTime -> notificationViewModel.updateSlotTime(context, slot, newTime) },
                        onSendTestNotification = { notificationViewModel.sendTestNotification(context) },
                        onClearInfoMessage = { notificationViewModel.clearInfoMessage() },
                        onRefreshExactAlarmPermission = { notificationViewModel.refreshExactAlarmPermission(context) },
                        onBackClick = {
                            authenticatedScreen = AuthenticatedScreen.HOME
                            homeViewModel.loadTodayDashboard()
                        },
                        modifier = modifier
                    )
                }
                AuthenticatedScreen.SHARE_CARD -> {
                    BackHandler {
                        authenticatedScreen = AuthenticatedScreen.HABIT_STREAK
                    }
                    val streakViewModel: StreakViewModel = viewModel()
                    val streakUiState by streakViewModel.uiState.collectAsState()
                    val habit = selectedHabit

                    if (habit != null) {
                        ShareCardScreen(
                            habit = habit,
                            lockIn = selectedLockIn,
                            currentStreak = streakUiState.currentStreak,
                            streakDays = streakUiState.currentStreakDays,
                            onBackClick = {
                                authenticatedScreen = AuthenticatedScreen.HABIT_STREAK
                            },
                            modifier = modifier
                        )
                    } else {
                        authenticatedScreen = AuthenticatedScreen.LOCK_INS
                    }
                }
                AuthenticatedScreen.DAILY_ROUTINE -> {
                    BackHandler {
                        authenticatedScreen = AuthenticatedScreen.HOME
                        homeViewModel.loadTodayDashboard()
                    }
                    val routineViewModel: DailyRoutineViewModel = viewModel()
                    val routineUiState by routineViewModel.uiState.collectAsState()

                    DailyRoutineScreen(
                        uiState = routineUiState,
                        onTitleChange = { routineViewModel.onTitleChange(it) },
                        onEmojiChange = { routineViewModel.onEmojiChange(it) },
                        onStartTimeChange = { routineViewModel.onStartTimeChange(it) },
                        onEndTimeChange = { routineViewModel.onEndTimeChange(it) },
                        onAddActivity = { routineViewModel.addActivity() },
                        onDeleteActivity = { routineViewModel.deleteActivity(it) },
                        onRefresh = { routineViewModel.loadTodayActivities() },
                        onNavigateToShareCard = {
                            authenticatedScreen = AuthenticatedScreen.DAILY_ROUTINE_SHARE_CARD
                        },
                        onBackClick = {
                            authenticatedScreen = AuthenticatedScreen.HOME
                            homeViewModel.loadTodayDashboard()
                        },
                        modifier = modifier
                    )
                }
                AuthenticatedScreen.DAILY_ROUTINE_SHARE_CARD -> {
                    BackHandler {
                        authenticatedScreen = AuthenticatedScreen.DAILY_ROUTINE
                    }
                    val routineViewModel: DailyRoutineViewModel = viewModel()
                    val routineUiState by routineViewModel.uiState.collectAsState()

                    DailyRoutineShareCardScreen(
                        activities = routineUiState.activities,
                        displayDate = routineUiState.displayDate,
                        onBackClick = {
                            authenticatedScreen = AuthenticatedScreen.DAILY_ROUTINE
                        },
                        modifier = modifier
                    )
                }
            }
        }
        is SessionStatus.NotAuthenticated,
        is SessionStatus.RefreshFailure -> {
            when (uiState.currentScreen) {
                AuthScreenType.LOGIN -> {
                    LoginScreen(
                        uiState = uiState,
                        onEmailChange = { viewModel.onLoginEmailChange(it) },
                        onPasswordChange = { viewModel.onLoginPasswordChange(it) },
                        onTogglePasswordVisibility = { viewModel.toggleLoginPasswordVisibility() },
                        onLoginClick = { viewModel.login() },
                        onNavigateToSignUp = { viewModel.switchToSignUp() },
                        modifier = modifier
                    )
                }
                AuthScreenType.SIGN_UP -> {
                    SignUpScreen(
                        uiState = uiState,
                        onEmailChange = { viewModel.onSignUpEmailChange(it) },
                        onPasswordChange = { viewModel.onSignUpPasswordChange(it) },
                        onConfirmPasswordChange = { viewModel.onSignUpConfirmPasswordChange(it) },
                        onTogglePasswordVisibility = { viewModel.toggleSignUpPasswordVisibility() },
                        onToggleConfirmPasswordVisibility = { viewModel.toggleSignUpConfirmPasswordVisibility() },
                        onSignUpClick = { viewModel.signUp() },
                        onNavigateToLogin = { viewModel.switchToLogin() },
                        modifier = modifier
                    )
                }
            }
        }
    }
}
