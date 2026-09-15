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
import com.nk.motificason.ui.home.HomeViewModel
import com.nk.motificason.ui.lockin.LockInScreen
import com.nk.motificason.ui.lockin.LockInViewModel

enum class AuthenticatedScreen {
    HOME,
    LOCK_INS
}

@Composable
fun MainApp(
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val sessionStatus by viewModel.sessionStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var authenticatedScreen by rememberSaveable { mutableStateOf(AuthenticatedScreen.HOME) }

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
