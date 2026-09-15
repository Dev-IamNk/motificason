# Motificason — Project Context

## What this app is
A gamified habit/discipline Android app. Users pick "Lock-Ins" (habit
categories like Coding Grind, Gym Mode), add habits under each, and
check in daily. Streaks track consistency with a freeze mechanic for
missed days. A calendar heatmap shows history per habit. Achievements
unlock at streak milestones. Two animated 9:16 share cards let users
post progress to Instagram/WhatsApp Stories: a Lock-In Journey (streak
grid filling in day by day) and a Daily Routine (clock face highlighting
time-blocked activities). Motivation tone (Friendly/Coach/Savage/
Brainrot) shapes notification and in-app message copy.

Phase 2 (not yet built): social layer — following, activity feed,
🔥 reactions, comments, scoped leaderboards.

## Tech stack
Kotlin, Jetpack Compose, Supabase (Postgres + Auth, RLS enabled on every
table, scoped to auth.uid() = user_id), Firebase Cloud Messaging for
notifications, Room for offline cache (not yet added). Package name:
com.nk.motificason.

## Conventions
- Every new Supabase table needs RLS enabled and a policy scoped to
  auth.uid() = user_id, same pattern as lock_ins and habits.
- Commit style: feat: / chore: / fix: prefix, one stage = one commit.
- Don't touch files outside the current stage's scope unless fixing a
  bug that blocks it.

## Build status
- [x] Stage 1: Auth (Supabase email/password, session persistence)
- [x] Stage 2: Lock-Ins & Habits
- [ ] Stage 3: Home dashboard + check-in
- [ ] Stage 4: Streak engine + freeze logic
- [ ] Stage 5: Calendar heatmap
- [ ] Stage 6: Achievements
- [ ] Stage 7: Notifications
- [ ] Stage 8: Share cards
- [ ] Stage 9: Offline cache
- [ ] Stage 10: Polish

## Instructions for the agent
Before building anything, check the current codebase and this status
list — don't assume, the code is the source of truth. After finishing
a stage, update the checkbox above yourself.
When fixing a bug, ask for the specific error/behavior if not given — don't guess broadly across the codebase.