package com.nk.motificason.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.motificason.data.AuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuthScreenType {
    LOGIN,
    SIGN_UP
}

data class AuthUiState(
    val currentScreen: AuthScreenType = AuthScreenType.LOGIN,
    val loginEmail: String = "",
    val loginPassword: String = "",
    val isLoginPasswordVisible: Boolean = false,
    val signUpEmail: String = "",
    val signUpPassword: String = "",
    val signUpConfirmPassword: String = "",
    val isSignUpPasswordVisible: Boolean = false,
    val isSignUpConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val sessionStatus: StateFlow<SessionStatus> = repository.sessionStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionStatus.Initializing
        )

    fun getCurrentUserEmail(): String? {
        return repository.getCurrentUser()?.email ?: repository.getCurrentSession()?.user?.email
    }

    fun onLoginEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(loginEmail = email, errorMessage = null)
    }

    fun onLoginPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(loginPassword = password, errorMessage = null)
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isLoginPasswordVisible = !_uiState.value.isLoginPasswordVisible
        )
    }

    fun onSignUpEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(signUpEmail = email, errorMessage = null)
    }

    fun onSignUpPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(signUpPassword = password, errorMessage = null)
    }

    fun onSignUpConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(signUpConfirmPassword = confirmPassword, errorMessage = null)
    }

    fun toggleSignUpPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isSignUpPasswordVisible = !_uiState.value.isSignUpPasswordVisible
        )
    }

    fun toggleSignUpConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isSignUpConfirmPasswordVisible = !_uiState.value.isSignUpConfirmPasswordVisible
        )
    }

    fun switchToSignUp() {
        _uiState.value = _uiState.value.copy(
            currentScreen = AuthScreenType.SIGN_UP,
            errorMessage = null,
            successMessage = null
        )
    }

    fun switchToLogin() {
        _uiState.value = _uiState.value.copy(
            currentScreen = AuthScreenType.LOGIN,
            errorMessage = null,
            successMessage = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun login() {
        val email = _uiState.value.loginEmail.trim()
        val password = _uiState.value.loginPassword

        if (email.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email address.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address.")
            return
        }
        if (password.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = repository.signIn(email, password)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginPassword = "",
                    errorMessage = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = parseAuthError(error)
                )
            }
        }
    }

    fun signUp() {
        val email = _uiState.value.signUpEmail.trim()
        val password = _uiState.value.signUpPassword
        val confirmPassword = _uiState.value.signUpConfirmPassword

        if (email.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter an email address.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address.")
            return
        }
        if (password.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a password.")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters long.")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            val result = repository.signUp(email, password)
            result.onSuccess { hasImmediateSession ->
                if (hasImmediateSession) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        signUpPassword = "",
                        signUpConfirmPassword = "",
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        signUpPassword = "",
                        signUpConfirmPassword = "",
                        errorMessage = null,
                        successMessage = "Sign up successful! Please check your email ($email) to confirm your account, then log in."
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = parseAuthError(error)
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.signOut()
            _uiState.value = AuthUiState()
        }
    }

    private fun parseAuthError(e: Throwable): String {
        val message = e.message ?: return "An unexpected error occurred. Please try again."
        val lower = message.lowercase()
        return when {
            "invalid login credentials" in lower || "invalid_grant" in lower || "invalid_credentials" in lower ->
                "Invalid email or password. Please verify your credentials and try again."
            "user already registered" in lower || "user_already_exists" in lower ->
                "An account with this email already exists. Please log in instead."
            "email not confirmed" in lower ->
                "Please confirm your email address before logging in. Check your inbox for the confirmation link."
            "password should be at least" in lower || "weak_password" in lower ->
                "Password must be at least 6 characters long."
            "unable to resolve host" in lower || "connectexception" in lower || "timeout" in lower || "failed to connect" in lower ->
                "Unable to connect to Supabase. Please check your internet connection."
            "rate limit" in lower || "too many requests" in lower ->
                "Too many requests. Please wait a few moments before trying again."
            else -> {
                // If it contains JSON like {"code":400,"msg":"..."}, extract clean message
                val clean = message.replace(Regex("""^RestException:\s*"""), "")
                if (clean.isNotBlank()) clean else "Authentication failed. Please try again."
            }
        }
    }
}
