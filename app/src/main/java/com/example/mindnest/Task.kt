package com.example.mindnest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.mindnest.data.entity.TaskEntity
import com.example.mindnest.utils.PreferenceManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)

    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    val tasks: LiveData<List<Task>> = _tasks

    init {
        loadTasks()
    }

    private fun loadTasks() {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            app.taskRepository.getTasksByUser(userId)
                .map { entities ->
                    entities.map { entity ->
                        Task(
                            id = entity.id,
                            title = entity.title,
                            description = entity.description,
                            completed = entity.completed,
                            createdAt = entity.createdAt
                        )
                    }
                }
                .collect { taskList ->
                    _tasks.value = taskList
                }
        }
    }

    fun addTask(title: String, description: String = "") {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            app.taskRepository.insertTask(
                TaskEntity(
                    id = 0,
                    userId = userId,
                    title = title,
                    description = description,
                    completed = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTask(task: Task) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || task.id == 0L) return

        viewModelScope.launch {
            app.taskRepository.deleteTaskById(task.id)
        }
    }

    fun updateTask(task: Task, newTitle: String) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || task.id == 0L) return

        viewModelScope.launch {
            app.taskRepository.updateTask(
                TaskEntity(
                    id = task.id,
                    userId = userId,
                    title = newTitle,
                    description = task.description,
                    completed = task.completed,
                    createdAt = task.createdAt
                )
            )
        }
    }


    fun setTaskCompletion(task: Task, isCompleted: Boolean) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || task.id == 0L) return

        viewModelScope.launch {
            app.taskRepository.updateTask(
                TaskEntity(
                    id = task.id,
                    userId = userId,
                    title = task.title,
                    description = task.description,
                    completed = isCompleted,
                    createdAt = task.createdAt
                )
            )
        }
    }
}
