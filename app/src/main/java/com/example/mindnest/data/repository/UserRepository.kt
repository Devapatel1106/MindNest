package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.UserDao
import com.example.mindnest.data.entity.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun login(email: String, password: String): User? {
        return userDao.getUserByEmailAndPassword(email, password)
    }

    suspend fun register(user: User): Long {
        val existingUser = userDao.getUserByEmail(user.email)
        return if (existingUser != null) {
            existingUser.id
        } else {
            userDao.insertUser(user)
        }
    }

    fun getUserById(userId: Long): Flow<User?> {
        return userDao.getUserById(userId)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }
}
