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
            .document(user.uid)
            .set(
                mapOf(
                    "id" to id,
                    "uid" to user.uid,
                    "name" to user.name,
                    "email" to user.email,
                    "password" to user.password,
                    "gender" to user.gender,
                    "createdAt" to user.createdAt
                )
            )
            .await()

        return id
    }


    suspend fun syncUserFromFirebase(uid: String): User? {

        val snapshot = firestore.collection("users")
            .document(uid)
            .get()
            .await()

        if (!snapshot.exists()) return null

        val email = snapshot.getString("email") ?: return null
        val name = snapshot.getString("name") ?: "User"
        val password = snapshot.getString("password") ?: ""
        val gender = snapshot.getString("gender") ?: ""
        val createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()

        val localUser = userDao.getUserByEmail(email)

        return if (localUser != null) {
            localUser
        } else {

            val newUser = User(
                uid = uid,
                name = name,
                email = email,
                password = password,
                gender = gender,
                createdAt = createdAt
            )

            val id = userDao.insertUser(newUser)
            newUser.copy(id = id)
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    fun getUserById(userId: Long): Flow<User?> {
        return userDao.getUserById(userId)
    }

    fun getUserGender(userId: Long): Flow<String?> {
        return userDao.getUserById(userId).map { it?.gender }
    }

    suspend fun updateUser(user: User) {

        userDao.updateUser(user)

        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "id" to user.id,
                    "uid" to user.uid,
                    "name" to user.name,
                    "email" to user.email,
                    "password" to user.password,
                    "gender" to user.gender,
                    "createdAt" to user.createdAt
                )
            )
            .await()
    }

    suspend fun deleteUser(user: User) {

        userDao.deleteUser(user)

        firestore.collection("users")
            .document(user.uid)
            .delete()
            .await()
    }
}