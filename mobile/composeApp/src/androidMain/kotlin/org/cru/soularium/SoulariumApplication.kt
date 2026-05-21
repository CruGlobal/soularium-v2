package org.cru.soularium

import android.app.Application
import org.cru.soularium.data.db.initDataAndroid
import org.cru.soularium.di.initKoin

class SoulariumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initDataAndroid(this)
        initKoin()
    }
}
