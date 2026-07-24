package org.cru.soularium.db.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

internal actual fun buildInMemorySoulariumDatabase(): SoulariumDatabase =
    Room.inMemoryDatabaseBuilder<SoulariumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
