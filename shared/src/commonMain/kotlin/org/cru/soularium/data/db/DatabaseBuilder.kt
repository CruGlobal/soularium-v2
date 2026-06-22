package org.cru.soularium.data.db

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<SoulariumDatabase>
