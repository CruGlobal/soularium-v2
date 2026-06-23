package org.cru.soularium.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private var appContext: Context? = null

fun initDataAndroid(context: Context) {
    appContext = context.applicationContext
}

/** The application context captured by [initDataAndroid], for module-internal use. */
internal fun dataAndroidContext(): Context = requireNotNull(appContext) {
    "initDataAndroid(context) must be called before the data layer is used."
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SoulariumDatabase> {
    val ctx = dataAndroidContext()
    val dbFile = ctx.getDatabasePath("soularium.db")
    return Room.databaseBuilder<SoulariumDatabase>(
        context = ctx,
        name = dbFile.absolutePath,
    )
}
