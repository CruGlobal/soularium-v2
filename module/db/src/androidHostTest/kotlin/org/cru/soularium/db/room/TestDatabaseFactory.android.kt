package org.cru.soularium.db.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

internal actual fun buildInMemorySoulariumDatabase(): SoulariumDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SoulariumDatabase::class.java)
        .allowMainThreadQueries()
        .build()
