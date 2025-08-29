# Task Manager Notification System

## Overview
This document describes the push notification system implemented for the Task Manager Android app. The system provides periodic reminders for pending tasks every 2 hours, along with due date alerts.

## Features

### 1. Periodic Task Reminders
- **Frequency**: Every 2 hours (configurable)
- **Content**: Shows pending incomplete tasks
- **Smart Filtering**: Excludes tasks overdue by more than 7 days
- **Adaptive Display**: Single task vs multiple tasks with different notification styles

### 2. Due Soon Alerts
- **Frequency**: Every 6 hours (configurable)
- **Content**: Tasks due within the next 24 hours
- **Priority**: High priority notifications with enhanced vibration

### 3. User Configuration
- **Settings Screen**: Complete UI for managing notification preferences
- **Customizable Intervals**: Multiple frequency options (1-24 hours)
- **Sound & Vibration**: Toggle controls for notification sounds and vibration
- **Enable/Disable**: Individual controls for different notification types

## Architecture

### Core Components

#### 1. Notification Manager (`TaskNotificationManager`)
- Creates and manages notification channels
- Formats notifications for single vs multiple tasks
- Handles notification styles (BigText, Inbox)
- Manages notification actions (Mark Complete)

#### 2. Work Manager Scheduler (`TaskNotificationScheduler`)
- Manages periodic WorkManager tasks
- Handles work cancellation and rescheduling
- Provides status checking for active schedules

#### 3. Background Worker (`TaskReminderWorker`)
- Executes background notification logic
- Fetches tasks from repository
- Filters and processes task data
- Triggers appropriate notifications

#### 4. Settings Management (`NotificationSettingsManager`)
- Persists user preferences in SharedPreferences
- Provides available interval options
- Manages all notification-related settings

#### 5. Permission Management (`NotificationPermissionManager`)
- Handles Android 13+ notification permissions
- Provides permission request utilities
- Manages permission state checking

### User Interface

#### 1. Notification Settings Screen (`NotificationSettingsScreen`)
- Complete settings interface with Material 3 design
- Dropdown menus for frequency selection
- Toggle switches for enabling/disabling features
- Test notification functionality
- Status indicators

#### 2. Enhanced Notifications Screen
- Added settings icon in top bar
- Navigation to notification settings
- Integration with existing notification display

### Data Flow

1. **App Launch**: `MainApplication` starts notification scheduling
2. **Background Execution**: WorkManager triggers `TaskReminderWorker`
3. **Data Retrieval**: Worker fetches tasks from `TaskRepository`
4. **Processing**: Tasks are filtered and categorized
5. **Notification Display**: `TaskNotificationManager` creates and shows notifications
6. **User Actions**: Notification actions (Mark Complete) are handled by `NotificationActionReceiver`

### Configuration Management

#### Use Case Integration (`ManageNotificationScheduleUseCase`)
- Centralizes notification management logic
- Handles settings updates and rescheduling
- Provides clean interface for UI components

#### Boot Persistence (`BootReceiver`)
- Restarts notification scheduling after device reboot
- Ensures continuity of notification service
- Handles app updates and reinstalls

## Implementation Details

### Permissions Required
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

### Notification Channels
- **Task Reminders**: Default priority, custom vibration pattern
- **Due Soon Alerts**: High priority, enhanced vibration

### WorkManager Configuration
- **Network**: Not required (works offline)
- **Battery**: Optimized constraints
- **Backoff Policy**: Linear with 15-30 minute delays
- **Persistence**: Survives app closure and device reboot

### Dependencies Added
- Firebase Messaging (already present)
- WorkManager (already present)
- Hilt integration for dependency injection

## Usage

### For Users
1. Navigate to Notifications → Settings (gear icon)
2. Configure desired reminder frequency
3. Enable/disable different notification types
4. Test notifications to verify setup
5. Customize sound and vibration preferences

### For Developers
1. Inject `ManageNotificationScheduleUseCase` in ViewModels
2. Use `TaskNotificationManager` for custom notifications
3. Extend `NotificationSettingsManager` for additional preferences
4. Monitor WorkManager execution for debugging

## Testing
- Use "Test Notification" button in settings
- Verify notification scheduling with WorkManager inspector
- Test permission handling on Android 13+
- Validate notification actions and navigation

## Future Enhancements
- Smart notification timing based on user activity
- Custom notification sounds
- Rich notification content with task details
- Integration with system focus modes
- Advanced filtering based on task priority
- Notification templates for different task types

## Files Created/Modified

### New Files
- `notification/TaskNotificationManager.kt`
- `notification/TaskNotificationScheduler.kt`
- `notification/TaskReminderWorker.kt`
- `notification/NotificationActionReceiver.kt`
- `notification/NotificationSettingsManager.kt`
- `notification/NotificationPermissionManager.kt`
- `notification/BootReceiver.kt`
- `presentation/notification/NotificationSettingsScreen.kt`
- `presentation/notification/NotificationSettingsViewModel.kt`
- `domain/usecase/notification/ManageNotificationScheduleUseCase.kt`

### Modified Files
- `AndroidManifest.xml` - Added permissions and receivers
- `MainApplication.kt` - Initialize notification scheduling
- `presentation/notification/NotificationsScreen.kt` - Added settings navigation
- `presentation/navigation/AppNavGraph.kt` - Added notification settings route
- `presentation/navigation/Screen.kt` - Added notification settings screen

This implementation provides a robust, user-friendly notification system that enhances user engagement and task completion rates.
