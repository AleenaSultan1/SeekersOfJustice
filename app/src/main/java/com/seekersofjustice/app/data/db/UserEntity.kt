package com.seekersofjustice.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seekersofjustice.app.domain.model.Officer

@Entity(
    tableName = "users",
    indices = [Index(value = ["name"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Salted PIN hash, hex. The raw PIN is never stored. */
    val pinHash: String,
    val pinSalt: String,
    val createdAt: Long,
)

fun UserEntity.toDomain() = Officer(id = id, name = name, createdAt = createdAt)
