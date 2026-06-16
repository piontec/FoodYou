package com.maksimowiczm.foodyou.app.infrastructure.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

internal actual val unlinkDiaryMigration: Migration =
    object : Migration(25, 26) {
        override fun migrate(connection: SQLiteConnection) = Unit
    }
