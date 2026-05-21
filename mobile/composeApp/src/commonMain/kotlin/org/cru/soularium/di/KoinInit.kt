package org.cru.soularium.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools

fun initKoin(extraModules: List<Module> = emptyList()) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    startKoin {
        modules(appModule, platformModule, *extraModules.toTypedArray())
    }
}
