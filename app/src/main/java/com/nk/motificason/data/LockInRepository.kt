package com.nk.motificason.data

import com.nk.motificason.data.model.CheckIn
import com.nk.motificason.data.model.CheckInInsert
import com.nk.motificason.data.model.CheckInUpdate
import com.nk.motificason.data.model.Habit
import com.nk.motificason.data.model.HabitInsert
import com.nk.motificason.data.model.HabitWithCheckIn
import com.nk.motificason.data.model.LockIn
import com.nk.motificason.data.model.LockInInsert
import com.nk.motificason.data.model.LockInWithHabitCheckIns
import com.nk.motificason.data.model.LockInWithHabits
import com.nk.motificason.data.local.AppDatabase
import com.nk.motificason.data.local.AppDatabaseProvider
import com.nk.motificason.data.local.CheckInEntity
import com.nk.motificason.data.local.HabitStreakEntity
import com.nk.motificason.data.local.toDomain
import com.nk.motificason.data.local.toEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class LockInRepository(
    private val db: AppDatabase? = null
) {
    private val client = SupabaseClientProvider.client
    private fun getDb(): AppDatabase? = db ?: AppDatabaseProvider.getDatabaseOrNull()

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

            val list = lockIns.map { lockIn ->
                LockInWithHabits(
                    lockIn = lockIn,
                    habits = habitsByLockInId[lockIn.id] ?: emptyList()
                )
            }
            syncLockInsWithHabitsToCache(userId, list)
            list
        }
    }

    suspend fun getTodayDashboard(userId: String, date: String): Result<List<LockInWithHabitCheckIns>> {
        return runCatching {
            val lockIns = client.from("lock_ins")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", order = Order.ASCENDING)
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

            val checkIns = client.from("check_ins")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("date", date)
                    }
                }
                .decodeList<CheckIn>()

            val checkInsByHabitId = checkIns.associateBy { it.habitId }
            val habitsByLockInId = habits.groupBy { it.lockInId }

            val dashboard = lockIns.map { lockIn ->
                val lockInHabits = habitsByLockInId[lockIn.id] ?: emptyList()
                val habitsWithCheckIn = lockInHabits.map { habit ->
                    val checkIn = checkInsByHabitId[habit.id]
                    HabitWithCheckIn(
                        habit = habit,
                        isCompleted = checkIn?.completed == true,
                        checkInId = checkIn?.id
                    )
                }
                LockInWithHabitCheckIns(
                    lockIn = lockIn,
                    habits = habitsWithCheckIn
                )
            }
            syncTodayDashboardToCache(userId, date, dashboard)
            dashboard
        }
    }

    suspend fun toggleCheckIn(
        habitId: String,
        userId: String,
        date: String,
        existingCheckInId: String?,
        newCompleted: Boolean
    ): Result<CheckIn> {
        return runCatching {
            if (!existingCheckInId.isNullOrBlank()) {
                client.from("check_ins")
                    .update(CheckInUpdate(completed = newCompleted)) {
                        select()
                        filter {
                            eq("id", existingCheckInId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeSingle<CheckIn>()
            } else {
                client.from("check_ins")
                    .insert(CheckInInsert(habitId = habitId, userId = userId, date = date, completed = newCompleted)) {
                        select()
                    }
                    .decodeSingle<CheckIn>()
            }
        }
    }

    suspend fun createLockIn(userId: String, name: String, emoji: String): Result<LockIn> {
        return runCatching {
            val created = client.from("lock_ins")
                .insert(LockInInsert(userId = userId, name = name, emoji = emoji)) {
                    select()
                }
                .decodeSingle<LockIn>()
            getDb()?.lockInDao()?.upsertLockIn(created.toEntity())
            created
        }
    }

    suspend fun createHabit(userId: String, lockInId: String, title: String): Result<Habit> {
        return runCatching {
            val created = client.from("habits")
                .insert(HabitInsert(lockInId = lockInId, userId = userId, title = title)) {
                    select()
                }
                .decodeSingle<Habit>()
            getDb()?.habitDao()?.upsertHabit(created.toEntity())
            created
        }
    }

    suspend fun getHabitStreak(
        habit: Habit,
        userId: String,
        today: java.time.LocalDate
    ): Result<com.nk.motificason.data.engine.StreakCalculationResult> {
        return runCatching {
            val checkIns = client.from("check_ins")
                .select {
                    filter {
                        eq("habit_id", habit.id)
                        eq("user_id", userId)
                    }
                }
                .decodeList<CheckIn>()

            val freezes = client.from("streak_freezes")
                .select {
                    filter {
                        eq("habit_id", habit.id)
                        eq("user_id", userId)
                    }
                }
                .decodeList<com.nk.motificason.data.model.StreakFreeze>()

            val habitCreatedAt = habit.createdAt?.let {
                runCatching { java.time.LocalDate.parse(it.substring(0, 10)) }.getOrNull()
            }

            val result = com.nk.motificason.data.engine.StreakEngine.calculate(
                today = today,
                habitCreatedAt = habitCreatedAt,
                checkIns = checkIns,
                freezes = freezes
            )

            // If any freezes were consumed in this run, persist them to Supabase
            for ((freeze, consumedDate) in result.freezesConsumedInThisRun) {
                client.from("streak_freezes")
                    .update(com.nk.motificason.data.model.StreakFreezeUpdate(usedOnDate = consumedDate.toString())) {
                        filter {
                            eq("id", freeze.id)
                            eq("user_id", userId)
                        }
                    }
            }

            // If milestone reached (multiple of 7) and freeze not yet awarded today, award new freeze
            if (result.shouldAwardMilestoneFreeze) {
                client.from("streak_freezes")
                    .insert(
                        com.nk.motificason.data.model.StreakFreezeInsert(
                            habitId = habit.id,
                            userId = userId,
                            earnedAt = today.toString(),
                            usedOnDate = null
                        )
                    )
            }

            saveHabitStreakToCache(habit.id, userId, result)
            result
        }
    }

    suspend fun getUserAchievements(userId: String): Result<List<com.nk.motificason.data.model.Achievement>> {
        return runCatching {
            client.from("achievements")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<com.nk.motificason.data.model.Achievement>()
        }
    }

    suspend fun evaluateAndUnlockAchievements(
        userId: String,
        targetHabitId: String? = null
    ): Result<List<com.nk.motificason.data.model.Achievement>> {
        return runCatching {
            val existing = client.from("achievements")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<com.nk.motificason.data.model.Achievement>()
                .toMutableList()

            val nowStr = java.time.LocalDate.now().toString()

            // 1. Check account-wide achievements
            val completedCheckIns = client.from("check_ins")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("completed", true)
                    }
                }
                .decodeList<CheckIn>()

            val totalCompleted = completedCheckIns.size

            // first_checkin
            if (totalCompleted >= 1 && existing.none { it.type == "first_checkin" }) {
                runCatching {
                    val created = client.from("achievements")
                        .insert(
                            com.nk.motificason.data.model.AchievementInsert(
                                userId = userId,
                                habitId = null,
                                type = "first_checkin",
                                unlockedAt = nowStr
                            )
                        ) {
                            select()
                        }
                        .decodeSingle<com.nk.motificason.data.model.Achievement>()
                    existing.add(created)
                }
            }

            // total_checkins_100
            if (totalCompleted >= 100 && existing.none { it.type == "total_checkins_100" }) {
                runCatching {
                    val created = client.from("achievements")
                        .insert(
                            com.nk.motificason.data.model.AchievementInsert(
                                userId = userId,
                                habitId = null,
                                type = "total_checkins_100",
                                unlockedAt = nowStr
                            )
                        ) {
                            select()
                        }
                        .decodeSingle<com.nk.motificason.data.model.Achievement>()
                    existing.add(created)
                }
            }

            // 2. Check per-habit streak achievements
            val habitsToCheck = if (!targetHabitId.isNullOrBlank()) {
                client.from("habits")
                    .select {
                        filter {
                            eq("id", targetHabitId)
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Habit>()
            } else {
                client.from("habits")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Habit>()
            }

            val today = java.time.LocalDate.now()
            val streakMilestones = listOf(
                Pair(7, "streak_7"),
                Pair(30, "streak_30"),
                Pair(100, "streak_100"),
                Pair(365, "streak_365")
            )

            for (habit in habitsToCheck) {
                val streakResult = getHabitStreak(habit, userId, today).getOrNull()
                val currentStreak = streakResult?.currentStreak ?: 0

                for ((target, type) in streakMilestones) {
                    if (currentStreak >= target && existing.none { it.habitId == habit.id && it.type == type }) {
                        runCatching {
                            val created = client.from("achievements")
                                .insert(
                                    com.nk.motificason.data.model.AchievementInsert(
                                        userId = userId,
                                        habitId = habit.id,
                                        type = type,
                                        unlockedAt = nowStr
                                    )
                                ) {
                                    select()
                                }
                                .decodeSingle<com.nk.motificason.data.model.Achievement>()
                            existing.add(created)
                        }
                    }
                }
            }

            existing
        }
    }

    // ==========================================
    // Room Offline Cache & Sync Methods (Stage 9)
    // ==========================================

    suspend fun getCachedTodayDashboard(userId: String, date: String): List<LockInWithHabitCheckIns> {
        val database = getDb() ?: return emptyList()
        return runCatching {
            val lockInEntities = database.lockInDao().getLockInsForUser(userId)
            val habitEntities = database.habitDao().getHabitsForUser(userId)
            val checkInEntities = database.checkInDao().getCheckInsForDate(userId, date)

            val checkInsByHabitId = checkInEntities.associateBy { it.habitId }
            val habitsByLockInId = habitEntities.groupBy { it.lockInId }

            lockInEntities.map { lockInEntity ->
                val habits = habitsByLockInId[lockInEntity.id] ?: emptyList()
                val habitsWithCheckIn = habits.map { habitEntity ->
                    val checkIn = checkInsByHabitId[habitEntity.id]
                    HabitWithCheckIn(
                        habit = habitEntity.toDomain(),
                        isCompleted = checkIn?.completed == true,
                        checkInId = checkIn?.id
                    )
                }
                LockInWithHabitCheckIns(
                    lockIn = lockInEntity.toDomain(),
                    habits = habitsWithCheckIn
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun syncTodayDashboardToCache(
        userId: String,
        date: String,
        remoteData: List<LockInWithHabitCheckIns>
    ) {
        val database = getDb() ?: return
        runCatching {
            val lockInsToSave = remoteData.map { it.lockIn.toEntity() }
            val habitsToSave = remoteData.flatMap { it.habits.map { h -> h.habit.toEntity() } }

            database.lockInDao().upsertLockIns(lockInsToSave)
            database.habitDao().upsertHabits(habitsToSave)

            // For check-ins: preserve any local check-in that is currently pending sync (synced == false)
            val checkInsToSave = mutableListOf<CheckInEntity>()
            for (group in remoteData) {
                for (h in group.habits) {
                    if (h.checkInId != null) {
                        val local = database.checkInDao().getCheckIn(h.habit.id, date)
                        if (local == null || local.synced) {
                            checkInsToSave.add(
                                CheckInEntity(
                                    id = h.checkInId,
                                    habitId = h.habit.id,
                                    userId = userId,
                                    date = date,
                                    completed = h.isCompleted,
                                    synced = true
                                )
                            )
                        }
                    }
                }
            }
            if (checkInsToSave.isNotEmpty()) {
                database.checkInDao().upsertCheckIns(checkInsToSave)
            }
        }
    }

    suspend fun getCachedLockInsWithHabits(userId: String): List<LockInWithHabits> {
        val database = getDb() ?: return emptyList()
        return runCatching {
            val lockIns = database.lockInDao().getLockInsForUser(userId)
            val habits = database.habitDao().getHabitsForUser(userId)
            val habitsByLockInId = habits.groupBy { it.lockInId }

            lockIns.map { lockIn ->
                LockInWithHabits(
                    lockIn = lockIn.toDomain(),
                    habits = (habitsByLockInId[lockIn.id] ?: emptyList()).map { it.toDomain() }
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun syncLockInsWithHabitsToCache(userId: String, remoteData: List<LockInWithHabits>) {
        val database = getDb() ?: return
        runCatching {
            val lockIns = remoteData.map { it.lockIn.toEntity() }
            val habits = remoteData.flatMap { it.habits.map { h -> h.toEntity() } }
            database.lockInDao().upsertLockIns(lockIns)
            database.habitDao().upsertHabits(habits)
        }
    }

    suspend fun getCachedHabitStreak(habitId: String): HabitStreakEntity? {
        val database = getDb() ?: return null
        return database.habitStreakDao().getStreak(habitId)
    }

    suspend fun saveHabitStreakToCache(
        habitId: String,
        userId: String,
        result: com.nk.motificason.data.engine.StreakCalculationResult
    ) {
        val database = getDb() ?: return
        runCatching {
            database.habitStreakDao().upsertStreak(
                HabitStreakEntity(
                    habitId = habitId,
                    userId = userId,
                    currentStreak = result.currentStreak,
                    longestStreak = result.longestStreak,
                    totalCompletedDays = result.totalCompletedDays,
                    unusedFreezeCount = result.unusedFreezeCount,
                    updatedAt = java.time.LocalDate.now().toString()
                )
            )
        }
    }

    suspend fun recordCheckInOffline(
        habitId: String,
        userId: String,
        date: String,
        existingCheckInId: String?,
        newCompleted: Boolean
    ): CheckIn {
        val database = getDb()
        val checkInId = existingCheckInId?.ifBlank { null } ?: java.util.UUID.randomUUID().toString()
        val entity = CheckInEntity(
            id = checkInId,
            habitId = habitId,
            userId = userId,
            date = date,
            completed = newCompleted,
            synced = false
        )
        database?.checkInDao()?.upsertCheckIn(entity)
        return entity.toDomain()
    }

    suspend fun markCheckInSynced(checkInId: String) {
        val database = getDb() ?: return
        database.checkInDao().markSynced(checkInId)
    }

    suspend fun syncPendingCheckIn(checkIn: CheckInEntity): Result<CheckIn> {
        return runCatching {
            val database = getDb()
            val remoteCheckIns = client.from("check_ins")
                .select {
                    filter {
                        eq("habit_id", checkIn.habitId)
                        eq("user_id", checkIn.userId)
                        eq("date", checkIn.date)
                    }
                }
                .decodeList<CheckIn>()

            val syncedCheckIn = if (remoteCheckIns.isNotEmpty()) {
                val existing = remoteCheckIns.first()
                val updated = client.from("check_ins")
                    .update(CheckInUpdate(completed = checkIn.completed)) {
                        select()
                        filter {
                            eq("id", existing.id)
                            eq("user_id", checkIn.userId)
                        }
                    }
                    .decodeSingle<CheckIn>()
                if (checkIn.id != existing.id) {
                    database?.checkInDao()?.updateIdAndMarkSynced(checkIn.id, existing.id)
                } else {
                    database?.checkInDao()?.markSynced(checkIn.id)
                }
                updated
            } else {
                val inserted = client.from("check_ins")
                    .insert(
                        CheckInInsert(
                            habitId = checkIn.habitId,
                            userId = checkIn.userId,
                            date = checkIn.date,
                            completed = checkIn.completed
                        )
                    ) {
                        select()
                    }
                    .decodeSingle<CheckIn>()
                database?.checkInDao()?.updateIdAndMarkSynced(checkIn.id, inserted.id)
                inserted
            }
            syncedCheckIn
        }
    }
}
