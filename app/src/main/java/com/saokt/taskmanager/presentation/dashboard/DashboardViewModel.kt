package com.saokt.taskmanager.presentation.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saokt.taskmanager.MainApplication
import com.saokt.taskmanager.data.remote.dto.ProjectDto
import com.saokt.taskmanager.data.remote.dto.ProjectInvitationDto
import com.saokt.taskmanager.data.remote.dto.ProjectMemberDto
import com.saokt.taskmanager.data.remote.dto.TaskDto
import com.saokt.taskmanager.domain.model.Project
import com.saokt.taskmanager.domain.model.Task
import com.saokt.taskmanager.domain.usecase.auth.SignOutUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.saokt.taskmanager.domain.usecase.remote.ListenToRemoteInvitationsUseCase
import com.saokt.taskmanager.domain.usecase.remote.ListenToRemoteProjectsUseCase
import com.saokt.taskmanager.domain.usecase.remote.ListenToRemoteTasksUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteInvitationsToLocalUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteMembersToLocalUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteProjectsToLocalUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteTasksToLocalUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskComplitionUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean // For thread-safe boolean check

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getLocalTasksUseCase: GetTasksUseCase,
    private val getLocalProjectsUseCase: GetProjectsUseCase,
    private val toggleTaskComplitionUseCase: ToggleTaskComplitionUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val listenToRemoteProjectsUseCase: ListenToRemoteProjectsUseCase,
    private val listenToRemoteTasksUseCase: ListenToRemoteTasksUseCase,
    private val listenToRemoteInvitationsUseCase: ListenToRemoteInvitationsUseCase, 
    private val syncRemoteProjectsToLocalUseCase: SyncRemoteProjectsToLocalUseCase,
    private val syncRemoteMembersToLocalUseCase: SyncRemoteMembersToLocalUseCase,
    private val syncRemoteTasksToLocalUseCase: SyncRemoteTasksToLocalUseCase,
    private val syncRemoteInvitationsToLocalUseCase: SyncRemoteInvitationsToLocalUseCase,
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
        getCurrentUserUseCase().distinctUntilChanged(), // Flow<User?>
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

    // Store latest data to process in correct order
    private val latestProjects = MutableStateFlow<List<ProjectDto>?>(null)
    private val latestMembers = MutableStateFlow<List<ProjectMemberDto>?>(null)
    private val latestTasks = MutableStateFlow<List<TaskDto>?>(null)
    private val latestInvitations = MutableStateFlow<List<ProjectInvitationDto>?>(null)

    // --- End Track initial sync ---

    init {
        Log.d("DashboardVM", "init block executing")
        observeAuthenticationAndTriggerSync()
        // No need to call observeLocalData separately, combine handles it
    }

    fun toggleTaskCompletion(task: Task) {
        (application as MainApplication).applicationScope.launch {
            val result = toggleTaskComplitionUseCase(task)
            if (result.isFailure) {
                Log.e("DashboardVM", "Failed to toggle task completion", result.exceptionOrNull())
                _error.value =
                    result.exceptionOrNull()?.localizedMessage ?: "Failed to toggle task completion"
            }
        }
    }


    private fun observeAuthenticationAndTriggerSync() {
        (application as MainApplication).applicationScope.launch {
            getCurrentUserUseCase()
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
                // Launch separate coroutines for each listener
                val projectsJob = launch {
                    listenToRemoteProjectsUseCase()
                        .collect { result ->
                            result.onSuccess { (projects, members) ->
                                Log.d("DashboardVM", "Received ${projects.size} projects and ${members.size} members")
                                latestProjects.value = projects
                                latestMembers.value = members
                                
                                // Process data in the correct order
                                processDataInOrder()
                            }.onFailure { e ->
                                Log.e("DashboardVM", "Error receiving projects", e)
                                _error.value = "Error receiving projects: ${e.message}"
                            }
                        }
                }
                
                val tasksJob = launch {
                    listenToRemoteTasksUseCase()
                        .collect { result ->
                            result.onSuccess { tasks ->
                                Log.d("DashboardVM", "Received ${tasks.size} tasks")
                                latestTasks.value = tasks
                                
                                // Process data in the correct order
                                processDataInOrder()
                            }.onFailure { e ->
                                Log.e("DashboardVM", "Error receiving tasks", e)
                                _error.value = "Error receiving tasks: ${e.message}"
                            }
                        }
                }
                
                val invitationsJob = launch {
                    listenToRemoteInvitationsUseCase()
                        .collect { result ->
                            result.onSuccess { invitations ->
                                Log.d("DashboardVM", "Received ${invitations.size} invitations")
                                latestInvitations.value = invitations
                                
                                // Process data in the correct order
                                processDataInOrder()
                            }.onFailure { e ->
                                Log.e("DashboardVM", "Error receiving invitations", e)
                                _error.value = "Error receiving invitations: ${e.message}"
                            }
                        }
                }
                
                // Wait for all jobs to complete (which should only happen if they're cancelled)
                projectsJob.join()
                tasksJob.join()
                invitationsJob.join()
                
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
    
    private suspend fun processDataInOrder() {
        (application as MainApplication).applicationScope.launch {
            // Only process if we have received projects (required for foreign key relationships)
            val projects = latestProjects.value ?: return@launch
            val members = latestMembers.value ?: return@launch
            
            // First sync projects (they don't depend on anything else)
            Log.d("DashboardVM", "Processing ${projects.size} projects")
            syncRemoteProjectsToLocalUseCase(projects)
            initialProjectsReceived.set(true)
            
            // Then sync members (depends on projects)
            Log.d("DashboardVM", "Processing ${members.size} members")
            syncRemoteMembersToLocalUseCase(members)
            
            // Then sync tasks if available (depends on projects)
            latestTasks.value?.let { tasks ->
                Log.d("DashboardVM", "Processing ${tasks.size} tasks")
                syncRemoteTasksToLocalUseCase(tasks)
                initialTasksReceived.set(true)
            }
            
            // Finally sync invitations if available 
            latestInvitations.value?.let { invitations ->
                Log.d("DashboardVM", "Processing ${invitations.size} invitations")
                syncRemoteInvitationsToLocalUseCase(invitations)
                initialInvitationsReceived.set(true)
            }
            
            // Check if sync is complete
            checkSyncComplete()
        }
    }
    
    private fun checkSyncComplete() {
        if (initialProjectsReceived.get() && initialTasksReceived.get() && initialInvitationsReceived.get()) {
            if (_isLoading.compareAndSet(true, false)) {
                Log.i("DashboardVM", "Initial sync COMPLETE for ALL types. isLoading = false.")
            }
        } else {
            Log.d(
                "DashboardVM",
                "Initial sync progress: Projects=${initialProjectsReceived.get()}, " +
                "Tasks=${initialTasksReceived.get()}, " +
                "Invitations=${initialInvitationsReceived.get()}"
            )
        }
    }

    private fun resetInitialSyncFlags() {
        initialProjectsReceived.set(false)
        initialTasksReceived.set(false)
        initialInvitationsReceived.set(false)
        
        // Clear latest data
        latestProjects.value = null
        latestMembers.value = null
        latestTasks.value = null
        latestInvitations.value = null

        Log.d("DashboardVM", "Initial sync flags reset.")
    }


    fun signOut() {
        (application as MainApplication).applicationScope.launch {
            listenerSyncJob?.cancel("Signing out") // Cancel listener before sign out
            Log.d("DashboardVM", "Signing out user...")
            val result = signOutUseCase() // Use the SignOutUseCase
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