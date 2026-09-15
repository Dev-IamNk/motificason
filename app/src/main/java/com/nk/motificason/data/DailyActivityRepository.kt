package com.nk.motificason.data

import com.nk.motificason.data.model.DailyActivity
import com.nk.motificason.data.model.DailyActivityInsert
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class DailyActivityRepository {
    private val client = SupabaseClientProvider.client

    suspend fun getTodayActivities(userId: String, date: String): Result<List<DailyActivity>> {
        return runCatching {
            client.from("daily_activities")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", date)
                    }
                    order("start_time", order = Order.ASCENDING)
                }
                .decodeList<DailyActivity>()
        }
    }

    suspend fun addActivity(
        userId: String,
        title: String,
        emoji: String,
        date: String,
        startTime: String,
        endTime: String
    ): Result<DailyActivity> {
        return runCatching {
            val insertItem = DailyActivityInsert(
                userId = userId,
                title = title,
                emoji = emoji,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
            client.from("daily_activities")
                .insert(insertItem) {
                    select()
                }
                .decodeSingle<DailyActivity>()
        }
    }

    suspend fun deleteActivity(activityId: String, userId: String): Result<Unit> {
        return runCatching {
            client.from("daily_activities")
                .delete {
                    filter {
                        eq("id", activityId)
                        eq("user_id", userId)
                    }
                }
        }
    }
}
