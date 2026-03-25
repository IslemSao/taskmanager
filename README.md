# Task Manager

Android task and project management app built with Kotlin, Jetpack Compose, Room, Hilt, WorkManager, and Firebase.

This repository is set up so you can plug in your own Firebase project and run the app locally without committing secrets.

## Features

- Email/password and Google sign-in
- Dashboard with task and project overview
- Project creation, editing, member invitations, and member management
- Task creation, assignment, subtasks, start/due dates, milestone tasks, priority tracking, workflow status, kanban board support, and Gantt-style timeline planning
- Advanced task filtering and sorting across status, assignment, priority, due date, and project
- Calendar view for due-date planning
- In-app notifications and notification settings
- Project and task chat
- App widget for top tasks
- Local Room persistence with background sync to Firebase

## Architecture Notes

- The app stays local-first: Room is the primary on-device source of truth, while Firebase listeners and sync workers reconcile remote changes back into local storage.
- Task querying is implemented as derived state in the presentation/domain layer. Screens observe full task flows, then apply a shared `TaskListQuery` through a reusable task query engine for filtering, sorting, counts, and board grouping.
- Workflow state is explicit through `TaskStatus` (`TODO`, `IN_PROGRESS`, `DONE`) and remains backward compatible with the legacy `completed` flag. Any task write path canonicalizes these fields so list, board, dashboard, calendar, widget, and sync behavior stay consistent.
- Timeline planning is also derived state. Tasks now carry `startDate`, `dueDate`, and `TaskType` (`TASK`, `MILESTONE`), then a shared timeline engine normalizes them into bars, milestone markers, unscheduled buckets, and editable schedule ranges for both personal and project views.
- UI state is kept in ViewModels. Composables render controls and invoke explicit events, but business rules like filtering, move permissions, and status synchronization stay outside the UI layer.

## Screenshots

- For open-source and portfolio presentation, prefer screenshots or short recordings that cover:
- the filtered task list with active chips and counts
- the project board view with todo / in progress / done columns
- the project timeline / Gantt view with draggable task bars and milestone markers
- the personal timeline view mixing personal and project work
- task detail editing with workflow status controls
- task detail editing with start date, due date, and milestone type
- cross-screen consistency between dashboard, calendar, and project task views

## Tech Stack

- Kotlin
- Jetpack Compose
- Hilt
- Room
- WorkManager
- Firebase Auth
- Cloud Firestore
- Firebase Cloud Messaging
- Glance App Widgets

## Local Setup

### 1. Android SDK

Set `ANDROID_HOME` to your Android SDK root, or create `local.properties` with:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

### 2. Firebase

Copy the sample config and replace the placeholder values with your own Firebase Android app config:

```bash
cp app/google-services.example.json app/google-services.json
```

Important notes:

- The Android package name is `com.saokt.taskmanager`
- Google sign-in uses the generated Firebase `default_web_client_id`, so it will follow your own `google-services.json`
- If Firebase config is missing, the app can still start in a limited local-only mode, but remote auth/sync features will not work correctly

### 3. Optional Release Signing

Debug builds do not require release signing.

If you want release builds:

```bash
cp keystore.properties.example keystore.properties
```

Then point `storeFile` to a local keystore path that is not committed.

### 4. Optional Firestore Email Update Script

If you want to run the helper script under `update-firestore-emails/`:

```bash
cp update-firestore-emails/serviceAccountKey.example.json update-firestore-emails/serviceAccountKey.json
```

## Run Verification

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

## Local UI Testing

The project includes local emulator UI tests for the authentication flow using Jetpack Compose UI testing.

Run the instrumented UI suite on a connected emulator or device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Useful outputs after the run:

- HTML report: `app/build/reports/androidTests/connected/debug/index.html`
- Device logs and raw results: `app/build/outputs/androidTest-results/connected/debug/`

When a Compose UI test fails, the helper will save a screenshot PNG for that failure so an AI tool can inspect what the emulator displayed during the broken state.

## Local Firebase Auth Integration Testing

The repo can also run real Firebase Auth + Firestore integration tests against the local Firebase emulators.

Run:

```bash
./scripts/run-firebase-auth-integration-tests.sh
```

That command will:

- start the local Firebase Auth and Firestore emulators
- run the Android instrumented tests with emulator wiring enabled
- shut the Firebase emulators down when the run finishes

## Open Source Notes

- Do not commit `google-services.json`, `keystore.properties`, keystores, or service account keys
- The repo includes example config files for local setup only
- If you fork this project, create your own Firebase project and update auth, Firestore, and FCM to match your configuration
- When extending task querying, start with the shared query model and engine under the task domain model package rather than adding ad hoc filtering in screens.
- When extending board behavior, keep new status transitions and permission rules centralized in the ViewModel/domain layer so the list and board views stay aligned.
- When extending timeline behavior, update the shared task timeline engine and scheduling use cases first so task detail, personal timeline, and project timeline stay behaviorally consistent.

## Current Focus

This repo is being cleaned up as an open-source portfolio project, with emphasis on:

- reliable feature behavior
- local onboarding
- secret-free setup
- better repo hygiene and documentation
