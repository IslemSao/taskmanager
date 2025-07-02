// data/mapper/ProjectMemberMapper.kt
package com.example.taskmanager.data.mapper

import com.example.taskmanager.data.local.entity.ProjectMemberEntity
import com.example.taskmanager.data.remote.dto.ProjectMemberDto
import com.example.taskmanager.domain.model.ProjectMember
import com.example.taskmanager.domain.model.ProjectRole
import java.util.Date
import javax.inject.Inject

class ProjectMemberMapper @Inject constructor() {    // This overload takes only the domain model
    fun domainToEntity(domain: ProjectMember): ProjectMemberEntity {
        return ProjectMemberEntity(
            projectId = "", // This needs to be populated by the caller
            userId = domain.userId,
            email = domain.email,
            displayName = domain.displayName,
            role = domain.role.name,
            joinedAt = domain.joinedAt
        )
    }

    // This overload takes both domain model and projectId
    fun domainToEntity(domain: ProjectMember, projectId: String): ProjectMemberEntity {
        return ProjectMemberEntity(
            projectId = projectId,
            userId = domain.userId,
            email = domain.email,
            displayName = domain.displayName,
            role = domain.role.name,
            joinedAt = domain.joinedAt
        )
    }

    fun entityToDomain(entity: ProjectMemberEntity): ProjectMember {
        return ProjectMember(
            projectId = entity.projectId,
            userId = entity.userId,
            email = entity.email,
            displayName = entity.displayName,
            role = ProjectRole.valueOf(entity.role),
            joinedAt = entity.joinedAt?: Date() // Default to current date if null
        )
    }

    fun domainToDto(domain: ProjectMember): ProjectMemberDto {
        return ProjectMemberDto(
            projectId = domain.projectId,
            userId = domain.userId,
            email = domain.email,
            displayName = domain.displayName,
            role = domain.role.name,
            joinedAt = domain.joinedAt
        )
    }    fun dtoToDomain(dto: ProjectMemberDto): ProjectMember {
        return ProjectMember(
            projectId = dto.projectId,
            userId = dto.userId,
            email = dto.email,
            displayName = dto.displayName,
            role = ProjectRole.valueOf(dto.role),
            joinedAt = dto.joinedAt
        )
    }
}
