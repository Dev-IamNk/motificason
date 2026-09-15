package com.nk.motificason.data

import com.nk.motificason.data.model.DEFAULT_MOTIVATION_MESSAGES
import com.nk.motificason.data.model.MotivationMessage
import com.nk.motificason.data.model.MotivationMessageInsert
import com.nk.motificason.data.model.MotivationTone
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class NotificationRepository(
    private val clientProvider: SupabaseClientProvider = SupabaseClientProvider
) {
    private val client get() = clientProvider.client

    suspend fun seedMessagesIfEmpty(): Result<Int> {
        return runCatching {
            val existing = client.from("messages")
                .select {
                    limit(1)
                }
                .decodeList<MotivationMessage>()

            if (existing.isEmpty()) {
                client.from("messages").insert(DEFAULT_MOTIVATION_MESSAGES)
                DEFAULT_MOTIVATION_MESSAGES.size
            } else {
                0
            }
        }
    }

    suspend fun getMotivationMessage(
        tone: MotivationTone,
        activeCategories: List<String>
    ): MotivationMessageInsert {
        // Attempt to fetch from Supabase messages table
        val remoteMessage = runCatching {
            val messages = client.from("messages")
                .select {
                    filter {
                        eq("active", true)
                        eq("tone", tone.id)
                    }
                }
                .decodeList<MotivationMessage>()

            if (messages.isNotEmpty()) {
                val categoryFiltered = if (activeCategories.isNotEmpty()) {
                    messages.filter { it.category in activeCategories }
                } else {
                    emptyList()
                }

                val selected = when {
                    categoryFiltered.isNotEmpty() -> categoryFiltered.random()
                    else -> messages.find { it.category.equals("General", ignoreCase = true) } ?: messages.random()
                }

                MotivationMessageInsert(
                    category = selected.category,
                    tone = selected.tone,
                    message = selected.message,
                    active = selected.active
                )
            } else {
                null
            }
        }.getOrNull()

        if (remoteMessage != null) {
            return remoteMessage
        }

        // Offline / Built-in fallback
        val toneMatches = DEFAULT_MOTIVATION_MESSAGES.filter {
            it.tone.equals(tone.id, ignoreCase = true)
        }

        val categoryMatches = if (activeCategories.isNotEmpty()) {
            toneMatches.filter { it.category in activeCategories }
        } else {
            emptyList()
        }

        return when {
            categoryMatches.isNotEmpty() -> categoryMatches.random()
            else -> toneMatches.find { it.category.equals("General", ignoreCase = true) }
                ?: toneMatches.firstOrNull()
                ?: DEFAULT_MOTIVATION_MESSAGES.first()
        }
    }
}
