package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.UserDao
import com.example.mindnest.data.entity.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val userDao: UserDao) {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): User? {
        return userDao.getUserByEmailAndPassword(email, password)
    }

    suspend fun register(user: User): Long {

        val existingUser = userDao.getUserByEmail(user.email)

        val id = if (existingUser != null) {
            existingUser.id
        } else {
            userDao.insertUser(user)
        }

        firestore.collection("users")
            .document(id.toString())
            .set(
                mapOf(
                    "id" to id,
                    "name" to user.name,
                    "email" to user.email,
                    "gender" to user.gender
                )
            )
            .await()

        return id
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

        firestore.collection("users")
            .document(user.id.toString())
            .set(user)
            .await()
    }

    suspend fun deleteUser(user: User) {

        userDao.deleteUser(user)

        firestore.collection("users")
            .document(user.id.toString())
            .delete()
            .await()
    }
}