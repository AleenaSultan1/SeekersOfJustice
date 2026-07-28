package com.seekersofjustice.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Insert
    suspend fun insert(user: UserEntity): Long
}
