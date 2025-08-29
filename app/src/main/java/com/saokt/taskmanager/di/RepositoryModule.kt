package com.saokt.taskmanager.di

import com.saokt.taskmanager.data.repository.ProjectRepositoryImpl
import com.saokt.taskmanager.data.repository.TaskRepositoryImpl
import com.saokt.taskmanager.data.repository.UserRepositoryImpl
import com.saokt.taskmanager.domain.repository.ProjectRepository
import com.saokt.taskmanager.domain.repository.TaskRepository
import com.saokt.taskmanager.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        projectRepositoryImpl: ProjectRepositoryImpl
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository


}
