package com.nk.motificason.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.StateFlow

class AuthRepository {
    private val auth = SupabaseClientProvider.client.auth

    val sessionStatus: StateFlow<SessionStatus> = auth.sessionStatus

    fun getCurrentSession(): UserSession? = auth.currentSessionOrNull()

    fun getCurrentUser(): UserInfo? = auth.currentUserOrNull()

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return runCatching {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
    }

    suspend fun signUp(email: String, password: String): Result<Boolean> {
        return runCatching {
            val user = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // If email confirmation is enabled on Supabase, currentSessionOrNull will be null
            // until confirmed. Returns true if directly authenticated, false if email confirmation is required.
            auth.currentSessionOrNull() != null
        }
    }

    suspend fun signOut(): Result<Unit> {
        return runCatching {
            auth.signOut()
        }
    }
}
