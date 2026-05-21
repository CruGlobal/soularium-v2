package org.cru.soularium.data.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun createDatabase(): SoulariumDatabase =
    getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .build()
