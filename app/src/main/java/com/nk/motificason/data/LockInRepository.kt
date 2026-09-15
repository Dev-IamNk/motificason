package com.nk.motificason.data

import com.nk.motificason.data.model.Habit
import com.nk.motificason.data.model.HabitInsert
import com.nk.motificason.data.model.LockIn
import com.nk.motificason.data.model.LockInInsert
import com.nk.motificason.data.model.LockInWithHabits
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class LockInRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getLockInsWithHabits(userId: String): Result<List<LockInWithHabits>> {
        return runCatching {
            val lockIns = client.from("lock_ins")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<LockIn>()

            val habits = client.from("habits")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", order = Order.ASCENDING)
                }
                .decodeList<Habit>()

            val habitsByLockInId = habits.groupBy { it.lockInId }

            lockIns.map { lockIn ->
                LockInWithHabits(
                    lockIn = lockIn,
                    habits = habitsByLockInId[lockIn.id] ?: emptyList()
                )
            }
        }
    }

    suspend fun createLockIn(userId: String, name: String, emoji: String): Result<LockIn> {
        return runCatching {
            client.from("lock_ins")
                .insert(LockInInsert(userId = userId, name = name, emoji = emoji)) {
                    select()
                }
                .decodeSingle<LockIn>()
        }
    }

    suspend fun createHabit(userId: String, lockInId: String, title: String): Result<Habit> {
        return runCatching {
            client.from("habits")
                .insert(HabitInsert(lockInId = lockInId, userId = userId, title = title)) {
                    select()
                }
                .decodeSingle<Habit>()
        }
    }
}
