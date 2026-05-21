package org.cru.soularium.platform

import android.content.Context

/**
 * Holds the application [Context] for platform services that need one (e.g.
 * [AndroidSharer]). Set once from `SoulariumApplication.onCreate()`; mirrors
 * the data module's own `initDataAndroid` context bootstrap.
 */
internal object AndroidAppContext {
    private var context: Context? = null

    fun set(value: Context) {
        context = value.applicationContext
    }

    fun get(): Context =
        requireNotNull(context) {
            "AndroidAppContext.set(context) must be called before get()."
        }
}
