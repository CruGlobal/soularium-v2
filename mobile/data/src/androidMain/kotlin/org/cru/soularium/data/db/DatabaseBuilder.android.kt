package org.cru.soularium.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private var appContext: Context? = null

fun initDataAndroid(context: Context) {
    appContext = context.applicationContext
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SoulariumDatabase> {
    val ctx =
        requireNotNull(appContext) {
            "initDataAndroid(context) must be called before getDatabaseBuilder()."
        }
    val dbFile = ctx.getDatabasePath("soularium.db")
    return Room.databaseBuilder<SoulariumDatabase>(
        context = ctx,
        name = dbFile.absolutePath,
    )
}
