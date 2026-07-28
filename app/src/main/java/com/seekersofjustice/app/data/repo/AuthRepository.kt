package com.seekersofjustice.app.data.repo

import com.seekersofjustice.app.data.auth.PinHasher
import com.seekersofjustice.app.data.db.UserDao
import com.seekersofjustice.app.data.db.UserEntity
import com.seekersofjustice.app.data.db.toDomain
import com.seekersofjustice.app.domain.model.Officer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(private val userDao: UserDao) {

    fun observeOfficers(): Flow<List<Officer>> =
        userDao.observeUsers().map { list -> list.map { it.toDomain() } }

    suspend fun hasAnyOfficer(): Boolean = userDao.count() > 0

    suspend fun createOfficer(name: String, pin: String): Officer {
        val salt = PinHasher.newSalt()
        val now = System.currentTimeMillis()
        val id = userDao.insert(
            UserEntity(
                name = name.trim(),
                pinHash = PinHasher.hash(pin, salt),
                pinSalt = salt,
                createdAt = now,
            )
        )
        return Officer(id = id, name = name.trim(), createdAt = now)
    }

    suspend fun verifyPin(officerId: Long, pin: String): Boolean {
        val user = userDao.getById(officerId) ?: return false
        return PinHasher.verify(pin, user.pinSalt, user.pinHash)
    }

    suspend fun getOfficer(officerId: Long): Officer? = userDao.getById(officerId)?.toDomain()
}
