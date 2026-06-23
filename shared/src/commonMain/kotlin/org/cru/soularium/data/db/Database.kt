package org.cru.soularium.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

fun createDatabase(): SoulariumDatabase = getDatabaseBuilder()
    .setDriver(BundledSQLiteDriver())
    .withForeignKeysEnabled()
    .build()

/**
 * SQLite enforces foreign keys — and therefore the entities' `ON DELETE
 * CASCADE` rules — only when `PRAGMA foreign_keys` is set, per connection.
 * Room with the bundled driver does not enable it automatically, so every
 * connection must opt in via this callback. Without it, deleting a session
 * would orphan its conversations and card picks instead of cascading.
 */
internal fun RoomDatabase.Builder<SoulariumDatabase>.withForeignKeysEnabled(): RoomDatabase.Builder<SoulariumDatabase> = addCallback(
    object : RoomDatabase.Callback() {
        override fun onOpen(connection: SQLiteConnection) {
            connection.execSQL("PRAGMA foreign_keys = ON")
        }
    },
)
