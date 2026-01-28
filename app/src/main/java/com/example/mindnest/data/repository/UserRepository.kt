package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.UserDao
import com.example.mindnest.data.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    fun getUserGender(userId: Long): Flow<String?> {
        return userDao.getUserById(userId).map { it?.gender }
    }

    fun getUsersByGender(gender: String): Flow<List<User>> {
        return userDao.getUsersByGender(gender)
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }
}
