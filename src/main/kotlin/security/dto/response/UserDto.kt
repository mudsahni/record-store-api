package com.muditsahni.security.dto.response

import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus
import java.time.Instant
import java.util.UUID

data class UserDto(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val tenantName: String,
    val roles: List<Role>,
    val status: UserStatus,
    val createdAt: Instant,
    val lastLoginAt: Instant?
    // ← Notice: NO passwordHash, no internal fields
) {
    companion object {
        fun from(user: User): UserDto {
            return UserDto(
                id = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                phoneNumber = user.phoneNumber,
                tenantName = user.tenantName,
                roles = user.roles,
                status = user.status,
                createdAt = user.createdAt,
                lastLoginAt = user.lastLoginAt
            )
        }
    }
}