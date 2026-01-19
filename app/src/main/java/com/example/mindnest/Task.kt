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
    val completed: Boolean = false
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
                            completed = entity.completed
                        )
                    }
                }
                .collect { taskList ->
                    _tasks.value = taskList
                }
        }
    }

    fun addTask(task: Task) {
        val userId = preferenceManager.getUserId()
        if (userId <= 0) return

        viewModelScope.launch {
            val entity = TaskEntity(
                id = 0,
                userId = userId,
                title = task.title,
                description = task.description,
                completed = task.completed
            )
            app.taskRepository.insertTask(entity)
        }
    }

    fun removeTask(position: Int) {
        val task = _tasks.value?.getOrNull(position) ?: return
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || task.id == 0L) return

        viewModelScope.launch {
            val entity = TaskEntity(
                id = task.id,
                userId = userId,
                title = task.title,
                description = task.description,
                completed = task.completed
            )
            app.taskRepository.deleteTask(entity)
        }
    }

    fun updateTask(position: Int, newTitle: String) {
        val task = _tasks.value?.getOrNull(position) ?: return
        val userId = preferenceManager.getUserId()
        if (userId <= 0 || task.id == 0L) return

        viewModelScope.launch {
            val entity = TaskEntity(
                id = task.id,
                userId = userId,
                title = newTitle,
                description = task.description,
                completed = task.completed
            )
            app.taskRepository.updateTask(entity)
        }
    }
}
