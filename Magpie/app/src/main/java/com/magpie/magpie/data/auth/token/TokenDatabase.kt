package com.magpie.magpie.data.auth.token

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TokenEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TokenDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
}
