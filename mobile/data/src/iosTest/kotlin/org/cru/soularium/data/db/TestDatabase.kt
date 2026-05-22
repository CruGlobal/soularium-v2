package org.cru.soularium.data.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Builds an in-memory [SoulariumDatabase] for tests, with foreign-key
 * enforcement enabled exactly as the production [createDatabase] configures it.
 */
fun inMemorySoulariumDatabase(): SoulariumDatabase =
    Room.inMemoryDatabaseBuilder<SoulariumDatabase>()
        .setDriver(BundledSQLiteDriver())
        .withForeignKeysEnabled()
        .build()
