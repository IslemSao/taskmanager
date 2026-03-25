package com.saokt.taskmanager.domain.di

import com.saokt.taskmanager.domain.repository.ProjectRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.ChatRepository
import com.saokt.taskmanager.domain.usecase.auth.CheckAuthStatusUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignInUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignInWithGoogleUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignOutUseCase
import com.saokt.taskmanager.domain.usecase.auth.SignUpUseCase
import com.saokt.taskmanager.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.saokt.taskmanager.domain.usecase.auth.SendEmailVerificationUseCase
import com.saokt.taskmanager.domain.usecase.auth.CheckEmailVerificationStatusUseCase
import com.saokt.taskmanager.domain.usecase.project.CreateProjectUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.saokt.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.saokt.taskmanager.domain.usecase.project.InviteProjectMemberUseCase
import com.saokt.taskmanager.domain.usecase.project.RemoveProjectMemberUseCase
import com.saokt.taskmanager.domain.usecase.project.UpdateProjectUseCase
import com.saokt.taskmanager.domain.usecase.remote.ListenToRemoteInvitationsUseCase
import com.saokt.taskmanager.domain.usecase.remote.ListenToRemoteProjectsUseCase
import com.saokt.taskmanager.domain.usecase.remote.ListenToRemoteTasksUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteInvitationsToLocalUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteMembersToLocalUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteProjectsToLocalUseCase
import com.saokt.taskmanager.domain.usecase.sync.SyncRemoteTasksToLocalUseCase
import com.saokt.taskmanager.domain.usecase.user.GetCurrentUserUseCase
import com.saokt.taskmanager.domain.usecase.task.AssignTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTaskByIdRemoteUseCase
import com.saokt.taskmanager.domain.usecase.task.CreateTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.DeleteTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksByProjectFromFirebaseUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksByProjectUseCase
import com.saokt.taskmanager.domain.usecase.task.GetTasksUseCase
import com.saokt.taskmanager.domain.usecase.task.MoveTaskToStatusUseCase
import com.saokt.taskmanager.domain.usecase.task.RescheduleTaskUseCase
import com.saokt.taskmanager.domain.usecase.task.ResizeTaskScheduleUseCase
import com.saokt.taskmanager.domain.usecase.task.ToggleTaskCompletionUseCase
import com.saokt.taskmanager.domain.usecase.task.UpdateTaskUseCase
import com.saokt.taskmanager.domain.usecase.chat.CreateOrGetThreadUseCase
import com.saokt.taskmanager.domain.usecase.chat.ListenMessagesUseCase
import com.saokt.taskmanager.domain.usecase.chat.SendMessageUseCase
import com.saokt.taskmanager.domain.usecase.chat.MarkThreadReadUseCase
import com.saokt.taskmanager.notification.FCMTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideCheckAuthStatusUseCase(userRepository: UserRepository): CheckAuthStatusUseCase {
        return CheckAuthStatusUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideSignInUseCase(userRepository: UserRepository): SignInUseCase {
        return SignInUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideSignInWithGoogleUseCase(userRepository: UserRepository): SignInWithGoogleUseCase {
        return SignInWithGoogleUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideSignUpUseCase(userRepository: UserRepository): SignUpUseCase {
        return SignUpUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideSendPasswordResetEmailUseCase(userRepository: UserRepository): SendPasswordResetEmailUseCase {
        return SendPasswordResetEmailUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideSendEmailVerificationUseCase(userRepository: UserRepository): SendEmailVerificationUseCase {
        return SendEmailVerificationUseCase(userRepository)
    }

    @Provides
    @Singleton
    fun provideCheckEmailVerificationStatusUseCase(userRepository: UserRepository): CheckEmailVerificationStatusUseCase {
        return CheckEmailVerificationStatusUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideSignOutUseCase(
        userRepository: UserRepository,
        fcmTokenManager: FCMTokenManager
    ): SignOutUseCase {
        return SignOutUseCase(userRepository, fcmTokenManager)
    }
    
    @Provides
    @Singleton
    fun provideGetCurrentUserUseCase(userRepository: UserRepository): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideListenToRemoteProjectsUseCase(userRepository: UserRepository): ListenToRemoteProjectsUseCase {
        return ListenToRemoteProjectsUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideListenToRemoteTasksUseCase(userRepository: UserRepository): ListenToRemoteTasksUseCase {
        return ListenToRemoteTasksUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideListenToRemoteInvitationsUseCase(userRepository: UserRepository): ListenToRemoteInvitationsUseCase {
        return ListenToRemoteInvitationsUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideSyncRemoteProjectsToLocalUseCase(userRepository: UserRepository): SyncRemoteProjectsToLocalUseCase {
        return SyncRemoteProjectsToLocalUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideSyncRemoteMembersToLocalUseCase(userRepository: UserRepository): SyncRemoteMembersToLocalUseCase {
        return SyncRemoteMembersToLocalUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideSyncRemoteTasksToLocalUseCase(userRepository: UserRepository): SyncRemoteTasksToLocalUseCase {
        return SyncRemoteTasksToLocalUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideSyncRemoteInvitationsToLocalUseCase(userRepository: UserRepository): SyncRemoteInvitationsToLocalUseCase {
        return SyncRemoteInvitationsToLocalUseCase(userRepository)
    }
    
    @Provides
    @Singleton
    fun provideAssignTaskUseCase(
        taskRepository: TaskRepository,
        userRepository: UserRepository
    ): AssignTaskUseCase {
        return AssignTaskUseCase(taskRepository, userRepository)
    }

    @Provides
    @Singleton
    fun provideGetTasksUseCase(taskRepository: TaskRepository): GetTasksUseCase {
        return GetTasksUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideGetTasksByProjectUseCase(taskRepository: TaskRepository): GetTasksByProjectUseCase {
        return GetTasksByProjectUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideToggleTaskCompletionUseCase(taskRepository: TaskRepository): ToggleTaskCompletionUseCase {
        return ToggleTaskCompletionUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteTaskUseCase(taskRepository: TaskRepository): DeleteTaskUseCase {
        return DeleteTaskUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideCreateTaskUseCase(
        taskRepository: TaskRepository,
        userRepository: UserRepository
    ): CreateTaskUseCase {
        return CreateTaskUseCase(taskRepository, userRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateTaskUseCase(
        taskRepository: TaskRepository,
        userRepository: UserRepository
    ): UpdateTaskUseCase {
        return UpdateTaskUseCase(taskRepository, userRepository)
    }

    @Provides
    @Singleton
    fun provideMoveTaskToStatusUseCase(taskRepository: TaskRepository): MoveTaskToStatusUseCase {
        return MoveTaskToStatusUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideRescheduleTaskUseCase(taskRepository: TaskRepository): RescheduleTaskUseCase {
        return RescheduleTaskUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideResizeTaskScheduleUseCase(taskRepository: TaskRepository): ResizeTaskScheduleUseCase {
        return ResizeTaskScheduleUseCase(taskRepository)
    }

    @Provides
    @Singleton
    fun provideGetTaskByIdRemoteUseCase(taskRepository: TaskRepository): GetTaskByIdRemoteUseCase {
        return GetTaskByIdRemoteUseCase(taskRepository)
    }
    
    @Provides
    @Singleton
    fun provideGetProjectsUseCase(projectRepository: ProjectRepository): GetProjectsUseCase {
        return GetProjectsUseCase(projectRepository)
    }
    
    @Provides
    @Singleton
    fun provideGetProjectByIdUseCase(projectRepository: ProjectRepository): GetProjectByIdUseCase {
        return GetProjectByIdUseCase(projectRepository)
    }
    
    @Provides
    @Singleton
    fun provideCreateProjectUseCase(projectRepository: ProjectRepository): CreateProjectUseCase {
        return CreateProjectUseCase(projectRepository)
    }
    
    @Provides
    @Singleton
    fun provideUpdateProjectUseCase(projectRepository: ProjectRepository): UpdateProjectUseCase {
        return UpdateProjectUseCase(projectRepository)
    }
    
    @Provides
    @Singleton
    fun provideInviteProjectMemberUseCase(projectRepository: ProjectRepository): InviteProjectMemberUseCase {
        return InviteProjectMemberUseCase(projectRepository)
    }
    
    @Provides
    @Singleton
    fun provideRemoveProjectMemberUseCase(projectRepository: ProjectRepository): RemoveProjectMemberUseCase {
        return RemoveProjectMemberUseCase(projectRepository)
    }
    
    @Provides
    @Singleton
    fun provideGetTasksByProjectFromFirebaseUseCase(taskRepository: TaskRepository): GetTasksByProjectFromFirebaseUseCase {
        return GetTasksByProjectFromFirebaseUseCase(taskRepository)
    }

    // Chat use cases
    @Provides
    @Singleton
    fun provideCreateOrGetThreadUseCase(chatRepository: ChatRepository): CreateOrGetThreadUseCase {
        return CreateOrGetThreadUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideListenMessagesUseCase(chatRepository: ChatRepository): ListenMessagesUseCase {
        return ListenMessagesUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideSendMessageUseCase(chatRepository: ChatRepository): SendMessageUseCase {
        return SendMessageUseCase(chatRepository)
    }

    @Provides
    @Singleton
    fun provideMarkThreadReadUseCase(chatRepository: ChatRepository): MarkThreadReadUseCase {
        return MarkThreadReadUseCase(chatRepository)
    }
} 
