# Room Database Integration Guide

## Overview
Room database has been set up for all modules. Here's how to integrate it:

## Database Structure
- **User**: Stores login/account information
- **TaskEntity**: Stores tasks
- **WorkoutEntity**: Stores workouts
- **WaterEntity**: Stores water intake entries
- **SleepEntity**: Stores sleep logs
- **JournalEntity**: Stores journal entries
- **PeriodEntity**: Stores period tracking data

## Integration Steps

### 1. Access Repository in Activities/Fragments
```kotlin
private val app by lazy { application as MindNestApplication }
private val preferenceManager by lazy { PreferenceManager(this) }
private val userId = preferenceManager.getUserId()
```

### 2. Update ViewModels to Use Repository
Example for TaskViewModel:
```kotlin
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MindNestApplication
    private val preferenceManager = PreferenceManager(application)
    
    private val userId = preferenceManager.getUserId()
    
    val tasks: LiveData<List<TaskEntity>> = app.taskRepository
        .getTasksByUser(userId)
        .asLiveData()
    
    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            app.taskRepository.insertTask(task.copy(userId = userId))
        }
    }
    
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            app.taskRepository.deleteTask(task)
        }
    }
}
```

### 3. Convert Domain Models to Entities
- Task -> TaskEntity (add userId)
- Workout -> WorkoutEntity (add userId)
- WaterEntry -> WaterEntity (add userId)
- etc.

### 4. Update Fragments to Observe LiveData
```kotlin
viewModel.tasks.observe(viewLifecycleOwner) { taskList ->
    // Update UI
}
```

## Current User Management
- User ID is stored in SharedPreferences via PreferenceManager
- Access via: `preferenceManager.getUserId()`
- Always include userId when creating entities

## Dashboard Data Usage
All data is now stored in Room database and can be queried for:
- Charts and graphs
- Statistics
- Historical data analysis
- Date range queries

Use repository methods with date ranges for dashboard visualizations.
