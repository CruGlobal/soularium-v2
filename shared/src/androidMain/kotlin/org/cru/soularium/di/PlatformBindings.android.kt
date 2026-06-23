package org.cru.soularium.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
actual class PlatformBindings(context: Context) {
    @Provides
    @SingleIn(AppScope::class)
    internal val context: Context = context
}
