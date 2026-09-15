package com.nk.motificason

import android.app.Application
import com.nk.motificason.data.local.AppDatabaseProvider
import com.nk.motificason.data.local.SyncManager

class MotificasonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabaseProvider.init(this)
        SyncManager.init(this)
    }
}
