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

@Composable
fun MainApp(
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val sessionStatus by viewModel.sessionStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

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
            HomeScreen(
                userEmail = viewModel.getCurrentUserEmail(),
                isLoading = uiState.isLoading,
                onLogoutClick = { viewModel.signOut() },
                modifier = modifier
            )
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
