package com.example.taskmanager.presentation.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanager.MainApplication
import com.example.taskmanager.data.remote.dto.ProjectDto
import com.example.taskmanager.data.remote.dto.ProjectInvitationDto
import com.example.taskmanager.data.remote.dto.ProjectMemberDto
import com.example.taskmanager.data.remote.dto.TaskDto
import com.example.taskmanager.domain.model.Project
import com.example.taskmanager.domain.model.Task
import com.example.taskmanager.domain.repository.UserRepository
import com.example.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.example.taskmanager.domain.usecase.task.GetTasksUseCase
import com.example.taskmanager.domain.usecase.task.ToggleTaskComplitionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicBoolean // For thread-safe boolean check

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getLocalTasksUseCase: GetTasksUseCase,
    private val getLocalProjectsUseCase: GetProjectsUseCase,
    private val toggleTaskComplitionUseCae: ToggleTaskComplitionUseCase,
    private val userRepository: UserRepository,
    private val application: Application
) : ViewModel() {

    // --- State Management Refined ---
    // Separate flows for loading and error status
    private val _isLoading = MutableStateFlow(true) // Start loading initially
    private val _error = MutableStateFlow<String?>(null)


    // Combine all data sources + status flows into the final UI state
    val state: StateFlow<DashboardState> = combine(
        getLocalTasksUseCase(),      // Flow<List<Task>> from Room
        getLocalProjectsUseCase(),     // Flow<List<Project>> from Room
        userRepository.getCurrentUser().distinctUntilChanged(), // Flow<User?>
        _isLoading,                  // Flow<Boolean>
        _error                       // Flow<String?>
    ) { tasks, projects, user, isLoading, error ->
        // This lambda runs whenever any of the source flows emit
        Log.v(
            "DashboardVM",
            "Combine producing state: isLoading=$isLoading, tasks=${tasks.size}, projects=${projects.size}, user=${user?.id != null}, error=$error"
        )
        DashboardState(
            // isLoading is now primarily driven by the _isLoading flow
            isLoading = isLoading,
            tasks = tasks,
            projects = projects,
            userName = user?.displayName ?: "User",
            // Don't show error if we are actively loading (might be transient)
            error = if (isLoading && error != null) null else error // Clear error display while loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = DashboardState(isLoading = true) // Initial state before combine runs
    )
    // --- End State Management ---


    private var listenerSyncJob: Job? = null // Job for managing the listener collection

    // --- Track initial sync status ---
    private val initialProjectsReceived = AtomicBoolean(false)
    private val initialTasksReceived = AtomicBoolean(false)
    private val initialInvitationsReceived = AtomicBoolean(false)

    // --- End Track initial sync ---

    init {
        Log.d("DashboardVM", "init block executing")
        observeAuthenticationAndTriggerSync()
        // No need to call observeLocalData separately, combine handles it
    }

    fun toggleTaskCompletion(task: Task) {
        (application as MainApplication).applicationScope.launch {
            val result = toggleTaskComplitionUseCae(task)
            if (result.isFailure) {
                Log.e("DashboardVM", "Failed to toggle task completion", result.exceptionOrNull())
                _error.value =
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to toggle task completion"
            }
        }
    }


    private fun observeAuthenticationAndTriggerSync() {
        (application as MainApplication).applicationScope.launch {
            userRepository.getCurrentUser()
                .distinctUntilChanged() // Prevent reacting to the same user object emission
                .collect { user ->
                    // Cancel previous listener job AND reset sync flags when auth changes
                    listenerSyncJob?.cancel("User auth state changed / restarting listeners")
                    resetInitialSyncFlags() // Reset flags for the new state

                    if (user != null) {
                        Log.i(
                            "DashboardVM",
                            "User authenticated (${user.id}). Starting listener job."
                        )
                        // User is logged in, launch data loading
                        listenerSyncJob = launchFirestoreListeners()
                    } else {
                        Log.i(
                            "DashboardVM",
                            "User logged out. Listener job should have been cancelled."
                        )
                        // Ensure loading/error are off if user logs out explicitly
                        _isLoading.value = false
                        _error.value = null
                    }
                }
        }
    }

    private fun launchFirestoreListeners(): Job {
        // 1. Reset flags and set loading ON
        resetInitialSyncFlags()
        _isLoading.value = true
        _error.value = null
        Log.d("DashboardVM", "launchFirestoreListeners: Starting new listener job.")

        return (application as MainApplication).applicationScope.launch {
            try {
                // 2. MAP each flow to the sealed type THEN merge
                merge(
                    userRepository.listenToRemoteProjects()
                        .map { result ->
                            result.map { (projects, members) ->
                                ListenerDataResult.ProjectsResult(projects, members)
                            }
                        },
                    userRepository.listenToRemoteTasks()
                        .map { result -> result.map { ListenerDataResult.TasksResult(it) } },
                    userRepository.listenToRemoteInvitations()
                        .map { result -> result.map { ListenerDataResult.InvitationsResult(it) } }
                )
                    .collect { result: Result<ListenerDataResult> ->
                        result.onSuccess { listenerData ->
                            var dataTypeProcessed: String? = null

                            // 3. Use 'when' on the sealed type to process and set flags
                            when (listenerData) {
                                is ListenerDataResult.ProjectsResult -> {
                                    val projects = listenerData.projects
                                    val members = listenerData.members
                                    Log.d(
                                        "DashboardVM",
                                        "Listener Flow: Received ${projects.size} Projects and ${members.size} Members."
                                    )
                                    userRepository.syncRemoteProjectsToLocal(projects)
                                    userRepository.syncRemoteMembersToLocal(members)
                                    initialProjectsReceived.set(true)
                                    dataTypeProcessed = "Projects & Members"
                                }

                                is ListenerDataResult.TasksResult -> {
                                    val tasks = listenerData.data
                                    Log.d("bombardiro", "tasks= : $tasks")
                                    Log.d(
                                        "DashboardVM",
                                        "Listener Flow: Received ${tasks.size} Tasks."
                                    )
                                    userRepository.syncRemoteTasksToLocal(tasks)
                                    initialTasksReceived.set(true)
                                    dataTypeProcessed = "Tasks"
                                }

                                is ListenerDataResult.InvitationsResult -> {
                                    val invitations = listenerData.data
                                    Log.d(
                                        "DashboardVM",
                                        "Listener Flow: Received ${invitations.size} Invitations."
                                    )
                                    userRepository.syncRemoteInvitationsToLocal(invitations)
                                    initialInvitationsReceived.set(true)
                                    dataTypeProcessed = "Invitations"
                                }
                            }

                            // 4. Check if ALL flags are now true to turn off loading
                            if (initialProjectsReceived.get() && initialTasksReceived.get() && initialInvitationsReceived.get()) {
                                if (_isLoading.compareAndSet(true, false)) {
                                    Log.i(
                                        "DashboardVM",
                                        "Initial sync COMPLETE for ALL types. isLoading = false."
                                    )
                                }
                            } else {
                                Log.d(
                                    "DashboardVM",
                                    "Initial sync progress: Projects=${initialProjectsReceived.get()}, Tasks=${initialTasksReceived.get()}, Invitations=${initialInvitationsReceived.get()} (Just processed: $dataTypeProcessed)"
                                )
                            }

                        }.onFailure { e ->
                            Log.e("DashboardVM", "Listener flow emitted failure Result", e)
                            _error.value = "Sync failed: ${e.localizedMessage}"
                            _isLoading.value = false
                        }
                    }
            } catch (ce: CancellationException) {
                Log.w("DashboardVM", "Listener collection coroutine CANCELLED.", ce)
                throw ce
            } catch (e: Exception) {
                Log.e("DashboardVM", "Listener collection coroutine failed UNEXPECTEDLY.", e)
                _error.value = "Unexpected sync error: ${e.localizedMessage}"
            }
        }.apply {
            invokeOnCompletion { throwable ->
                Log.i("DashboardVM", "Listener Job $this completed.")
                _isLoading.compareAndSet(true, false)

                if (throwable != null && throwable !is CancellationException) {
                    Log.e(
                        "DashboardVM",
                        "Listener Job $this finished with error (invokeOnCompletion)",
                        throwable
                    )
                    if (_error.value == null) {
                        _error.value = "Listener failed: ${throwable.localizedMessage}"
                    }
                } else if (throwable is CancellationException) {
                    Log.w(
                        "DashboardVM",
                        "Listener Job $this cancelled (invokeOnCompletion). Cause: ${throwable.cause}"
                    )
                } else {
                    Log.d("DashboardVM", "Listener Job $this finished successfully.")
                }
            }
        }
    }

    private fun resetInitialSyncFlags() {
        initialProjectsReceived.set(false)
        initialTasksReceived.set(false)
        initialInvitationsReceived.set(false)

        Log.d("DashboardVM", "Initial sync flags reset.")
    }


    fun signOut() {
        (application as MainApplication).applicationScope.launch {
            listenerSyncJob?.cancel("Signing out") // Cancel listener before sign out
            Log.d("DashboardVM", "Signing out user...")
            val result =
                userRepository.signOut() // Clears local DB, triggers auth observer -> null user
            if (result.isFailure) {
                Log.e("DashboardVM", "Sign out failed", result.exceptionOrNull())
                _error.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to sign out"
            }
            // State resets via auth observer collecting null user
        }
    }

    // Optional: Manual Refresh
    fun refreshData() {
        Log.d("DashboardVM", "Manual refresh triggered.")
        listenerSyncJob?.cancel("Manual refresh requested")
        // Reset flags and restart listeners IF user is still logged in
        // Checking auth state here synchronously is tricky, relying on the observer is safer
        // If the user is logged in, the observer *should* restart the listeners automatically.
        // If needed, you could expose a way to re-trigger the observer logic.
        // Forcing a restart:
        resetInitialSyncFlags()
        _isLoading.value = true // Manually set loading true for refresh
        listenerSyncJob = launchFirestoreListeners() // Relaunch the listener job
    }


    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        Log.d("DashboardVM", "onCleared called")
        super.onCleared()
    }
}

// DashboardState remains the same
data class DashboardState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val userName: String = "",
    val error: String? = null
)

sealed class ListenerDataResult {
    data class ProjectsResult(
        val projects: List<ProjectDto>,
        val members: List<ProjectMemberDto>
    ) : ListenerDataResult()

    data class TasksResult(val data: List<TaskDto>) : ListenerDataResult()
    data class InvitationsResult(val data: List<ProjectInvitationDto>) : ListenerDataResult()
}
// Make sure FirebaseAuth is injectable via Hilt
// Add to your Hilt module:
// @Provides
// @Singleton
// fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth