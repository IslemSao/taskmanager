// data/mapper/ProjectInvitationMapper.kt
package com.example.taskmanager.data.mapper

import com.example.taskmanager.data.local.entity.ProjectInvitationEntity
import com.example.taskmanager.data.remote.dto.ProjectInvitationDto
import com.example.taskmanager.domain.model.InvitationStatus
import com.example.taskmanager.domain.model.ProjectInvitation
import javax.inject.Inject

class ProjectInvitationMapper @Inject constructor() {
    fun domainToEntity(domain: ProjectInvitation): ProjectInvitationEntity {
        return ProjectInvitationEntity(
            id = domain.id,
            projectId = domain.projectId,
            projectTitle = domain.projectTitle,
            inviterId = domain.inviterId,
            inviterName = domain.inviterName,
            inviteeId = domain.inviteeId,
            inviteeEmail = domain.inviteeEmail,
            status = domain.status.name,
            createdAt = domain.createdAt
        )
    }

    fun entityToDomain(entity: ProjectInvitationEntity): ProjectInvitation {
        return ProjectInvitation(
            id = entity.id,
            projectId = entity.projectId,
            projectTitle = entity.projectTitle,
            inviterId = entity.inviterId,
            inviterName = entity.inviterName,
            inviteeId = entity.inviteeId,
            inviteeEmail = entity.inviteeEmail,
            status = InvitationStatus.valueOf(entity.status),
            createdAt = entity.createdAt
        )
    }

    fun domainToDto(domain: ProjectInvitation): ProjectInvitationDto {
        return ProjectInvitationDto(
            id = domain.id,
            projectId = domain.projectId,
            projectTitle = domain.projectTitle,
            inviterId = domain.inviterId,
            inviterName = domain.inviterName,
            inviteeId = domain.inviteeId,
            inviteeEmail = domain.inviteeEmail,
            status = domain.status.name,
            createdAt = domain.createdAt
        )
    }

    fun dtoToDomain(dto: ProjectInvitationDto): ProjectInvitation {
        return ProjectInvitation(
            id = dto.id,
            projectId = dto.projectId,
            projectTitle = dto.projectTitle,
            inviterId = dto.inviterId,
            inviterName = dto.inviterName,
            inviteeId = dto.inviteeId,
            inviteeEmail = dto.inviteeEmail,
            status = InvitationStatus.valueOf(dto.status),
            createdAt = dto.createdAt
        )
    }
}
