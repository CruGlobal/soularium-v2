package org.cru.soularium.platform

import android.content.Context

/**
 * Holds the application [Context] for platform services that need one (e.g.
 * [AndroidSharer]). Set once from `SoulariumApplication.onCreate()` in the
 * `:androidApp` module; mirrors the data module's own `initDataAndroid`
 * context bootstrap.
 */
object AndroidAppContext {
    private var context: Context? = null

    fun set(value: Context) {
        context = value.applicationContext
    }

    fun get(): Context =
        requireNotNull(context) {
            "AndroidAppContext.set(context) must be called before get()."
        }
}
