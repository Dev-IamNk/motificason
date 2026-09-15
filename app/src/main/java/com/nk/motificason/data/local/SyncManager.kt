package com.nk.motificason.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.nk.motificason.data.LockInRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object SyncManager {
    private const val TAG = "SyncManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repository: LockInRepository? = null

    private val _syncCompletedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val syncCompletedEvents: SharedFlow<Unit> = _syncCompletedEvents.asSharedFlow()

    @Volatile
    private var isSyncing = false

    fun init(context: Context, lockInRepository: LockInRepository = LockInRepository()) {
        repository = lockInRepository
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "Network connection restored — triggering sync pass")
                        triggerSync()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }

        // Trigger an initial sync pass
        triggerSync()
    }

    fun triggerSync() {
        if (isSyncing) return
        scope.launch {
            try {
                isSyncing = true
                val db = AppDatabaseProvider.getDatabaseOrNull() ?: return@launch
                val repo = repository ?: return@launch
                val checkInDao = db.checkInDao()

                val pendingCheckIns = checkInDao.getAllUnsyncedCheckIns()
                if (pendingCheckIns.isEmpty()) {
                    return@launch
                }

                Log.d(TAG, "Syncing ${pendingCheckIns.size} pending check-ins to Supabase...")
                var anySynced = false

                for (checkIn in pendingCheckIns) {
                    val result = repo.syncPendingCheckIn(checkIn)
                    if (result.isSuccess) {
                        anySynced = true
                        Log.d(TAG, "Successfully synced check-in: ${checkIn.habitId} (${checkIn.date})")
                    } else {
                        Log.w(TAG, "Failed to sync check-in: ${checkIn.habitId} (${checkIn.date})", result.exceptionOrNull())
                    }
                }

                if (anySynced) {
                    _syncCompletedEvents.tryEmit(Unit)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sync pass", e)
            } finally {
                isSyncing = false
            }
        }
    }
}
