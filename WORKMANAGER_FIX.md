# WorkManager Circular Dependency Fix

## Problem
The app was crashing on startup with the error:
```
kotlin.UninitializedPropertyAccessException: lateinit property workerFactory has not been initialized
```

## Root Cause
There was a circular dependency in the initialization order:

1. `MainApplication.onCreate()` called `WorkManager.initialize()` immediately
2. `WorkManager.initialize()` needed `workManagerConfiguration` 
3. `workManagerConfiguration` needed `workerFactory` (injected by Hilt)
4. But Hilt injection happens **after** `onCreate()` completes
5. Meanwhile, `WorkManagerModule` was trying to get `WorkManager.getInstance()` during dependency injection
6. This created a circular dependency: WorkManager needs Application → Application needs WorkManager

## Solution
1. **Removed WorkManagerModule**: Eliminated the circular dependency by not injecting WorkManager
2. **Removed early WorkManager.initialize()**: Let WorkManager initialize automatically when first accessed
3. **Updated TaskNotificationScheduler**: Changed from injected WorkManager to `WorkManager.getInstance(context)`
4. **Preserved Configuration.Provider**: App still provides custom worker factory for Hilt workers

## Changes Made

### 1. Deleted WorkManagerModule.kt
```kotlin
// REMOVED: This was causing circular dependency
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context) // This caused the circular dependency
    }
}
```

### 2. Updated MainApplication.kt
```kotlin
// BEFORE:
override fun onCreate() {
    super.onCreate()
    WorkManager.initialize(this, workManagerConfiguration) // Too early!
    // ...
}

// AFTER:  
override fun onCreate() {
    super.onCreate()
    // Removed early initialization - WorkManager initializes automatically
    // ...
}
```

### 3. Updated TaskNotificationScheduler.kt
```kotlin
// BEFORE:
class TaskNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager // Injected - caused circular dependency
)

// AFTER:
class TaskNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
    // No WorkManager injection - get instance directly
)

// All methods now use:
WorkManager.getInstance(context).enqueueUniquePeriodicWork(...)
```

## How It Works Now
1. App starts and Hilt initializes all dependencies
2. `TaskNotificationScheduler` gets injected without WorkManager dependency
3. When notification scheduling is needed, `WorkManager.getInstance(context)` is called
4. WorkManager initializes automatically using the custom configuration from `Configuration.Provider`
5. Custom `HiltWorkerFactory` is available because injection is complete

## Result
- ✅ **No more crashes** - App starts successfully
- ✅ **Build successful** - All compilation errors resolved  
- ✅ **Hilt integration preserved** - `@HiltWorker` still works
- ✅ **Custom worker factory** - Background workers get proper dependency injection
- ✅ **Notification system intact** - All notification features work as designed

## Key Lesson
When using WorkManager with Hilt:
- Avoid injecting WorkManager as a dependency
- Use `WorkManager.getInstance(context)` directly in classes that need it
- Let WorkManager initialize lazily when first accessed
- Keep `Configuration.Provider` for custom worker factory setup
