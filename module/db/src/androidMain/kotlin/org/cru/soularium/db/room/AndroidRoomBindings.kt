package org.cru.soularium.db.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@BindingContainer
@ContributesTo(AppScope::class)
object AndroidRoomBindings {
    @Provides
    internal fun providesDatabaseBuilder(context: Context): RoomDatabase.Builder<SoulariumDatabase> =
        Room.databaseBuilder<SoulariumDatabase>(
            context = context,
            name = context.getDatabasePath("soularium.db").absolutePath,
        ).setDriver(AndroidSQLiteDriver())
}
