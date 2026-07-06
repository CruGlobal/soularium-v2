package org.cru.soularium.di

import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.createGraphFactory
import org.cru.soularium.domain.ports.DeviceStateRepository

@DependencyGraph(AppScope::class)
interface SoulariumAppGraph : LoggingBindings.Accessors {
    val circuit: Circuit
    val deviceStateRepo: DeviceStateRepository

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Includes platformBindings: PlatformBindings): SoulariumAppGraph
    }
}

fun createSoulariumAppGraph(platformBindings: PlatformBindings): SoulariumAppGraph =
    createGraphFactory<SoulariumAppGraph.Factory>().create(platformBindings)
