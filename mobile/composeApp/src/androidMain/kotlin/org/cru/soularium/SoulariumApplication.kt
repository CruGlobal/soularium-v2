package org.cru.soularium

import android.app.Application
import org.cru.soularium.data.db.initDataAndroid
import org.cru.soularium.di.initKoin
import org.cru.soularium.platform.AndroidAppContext

class SoulariumApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.set(this)
        initDataAndroid(this)
        initKoin()
    }
}
