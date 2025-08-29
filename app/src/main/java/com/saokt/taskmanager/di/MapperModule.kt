// di/MapperModule.kt
package com.saokt.taskmanager.di

import com.saokt.taskmanager.data.mapper.ProjectInvitationMapper
import com.saokt.taskmanager.data.mapper.ProjectMemberMapper
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
