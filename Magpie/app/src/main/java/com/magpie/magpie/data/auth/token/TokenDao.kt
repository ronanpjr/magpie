package com.magpie.magpie.data.auth.token

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TokenDao {
    @Query("SELECT * FROM auth_token WHERE id = 0 LIMIT 1")
    suspend fun getToken(): TokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertToken(token: TokenEntity)

    @Query("DELETE FROM auth_token WHERE id = 0")
    suspend fun clearToken()
}
