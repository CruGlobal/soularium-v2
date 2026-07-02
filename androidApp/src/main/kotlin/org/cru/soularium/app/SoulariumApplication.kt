package org.cru.soularium.app

import android.app.Application
import dev.zacsweers.metro.asContribution
import org.cru.soularium.di.LoggingBindings
import org.cru.soularium.di.PlatformBindings
import org.cru.soularium.di.SoulariumAppGraph
import org.cru.soularium.di.configureLogging
import org.cru.soularium.di.createSoulariumAppGraph

class SoulariumApplication : Application() {
    lateinit var appGraph: SoulariumAppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createSoulariumAppGraph(PlatformBindings(this))
        appGraph.asContribution<LoggingBindings.Accessors>().configureLogging()
    }
}
