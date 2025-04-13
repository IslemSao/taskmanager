package com.example.taskmanager.domain.di

import com.example.taskmanager.domain.repository.ProjectRepository
import com.example.taskmanager.domain.repository.UserRepository
import com.example.taskmanager.domain.usecase.auth.CheckAuthStatusUseCase
import com.example.taskmanager.domain.usecase.auth.SignInUseCase
import com.example.taskmanager.domain.usecase.auth.SignInWithGoogleUseCase
import com.example.taskmanager.domain.usecase.auth.SignOutUseCase
import com.example.taskmanager.domain.usecase.auth.SignUpUseCase
import com.example.taskmanager.domain.usecase.project.CreateProjectUseCase
import com.example.taskmanager.domain.usecase.project.GetProjectByIdUseCase
import com.example.taskmanager.domain.usecase.project.GetProjectsUseCase
import com.example.taskmanager.domain.usecase.project.InviteProjectMemberUseCase
import com.example.taskmanager.domain.usecase.project.RemoveProjectMemberUseCase
import com.example.taskmanager.domain.usecase.project.UpdateProjectUseCase
import com.example.taskmanager.domain.usecase.remote.ListenToRemoteInvitationsUseCase
import com.example.taskmanager.domain.usecase.remote.ListenToRemoteProjectsUseCase
import com.example.taskmanager.domain.usecase.remote.ListenToRemoteTasksUseCase
import com.example.taskmanager.domain.usecase.sync.SyncRemoteInvitationsToLocalUseCase
import com.example.taskmanager.domain.usecase.sync.SyncRemoteMembersToLocalUseCase
import com.example.taskmanager.domain.usecase.sync.SyncRemoteProjectsToLocalUseCase
import com.example.taskmanager.domain.usecase.sync.SyncRemoteTasksToLocalUseCase
import com.example.taskmanager.domain.usecase.user.GetCurrentUserUseCase
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
    fun provideSignOutUseCase(userRepository: UserRepository): SignOutUseCase {
        return SignOutUseCase(userRepository)
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
} 