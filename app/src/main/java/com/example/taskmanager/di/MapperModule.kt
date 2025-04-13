// di/MapperModule.kt
package com.example.taskmanager.di

import com.example.taskmanager.data.mapper.ProjectInvitationMapper
import com.example.taskmanager.data.mapper.ProjectMemberMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    @Provides
    @Singleton
    fun provideProjectMemberMapper(): ProjectMemberMapper {
        return ProjectMemberMapper()
    }

    @Provides
    @Singleton
    fun provideProjectInvitationMapper(): ProjectInvitationMapper {
        return ProjectInvitationMapper()
    }
}
