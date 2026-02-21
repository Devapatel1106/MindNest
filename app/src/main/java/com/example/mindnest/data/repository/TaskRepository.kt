package com.example.mindnest.data.repository

import com.example.mindnest.data.dao.TaskDao
import com.example.mindnest.data.entity.TaskEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getTasksByUser(userId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksByUser(userId)
    }

    suspend fun insertTask(task: TaskEntity): Long {

        val id = taskDao.insertTask(task)
        syncTaskToFirebase(task.copy(id = id))

        return id
    }

    suspend fun updateTask(task: TaskEntity) {

        taskDao.updateTask(task)

        syncTaskToFirebase(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
        deleteFromFirebase(task.id)
    }

    suspend fun deleteTaskById(taskId: Long) {
        taskDao.deleteTaskById(taskId)
        deleteFromFirebase(taskId)
    }


    private fun syncTaskToFirebase(task: TaskEntity) {

        val uid = auth.currentUser?.uid ?: return

        val taskMap = hashMapOf(
            "id" to task.id,
            "userId" to task.userId,
            "title" to task.title,
            "description" to task.description,
            "createdAt" to task.createdAt,
            "completed" to task.completed,
            "date" to task.date
        )

        firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .document(task.id.toString())
            .set(taskMap)
    }

    private fun deleteFromFirebase(taskId: Long) {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("tasks")
            .document(taskId.toString())
            .delete()
    }
}
