package com.muditsahni.security.dto.response

import com.muditsahni.model.entity.Tenant
import java.time.Instant
import java.util.*

data class TenantDto(
    val id: UUID,
    val name: String,
    val type: String,
    val createdAt: Instant,
    val isActive: Boolean
) {
    companion object {
        fun from(tenant: Tenant): TenantDto {
            return TenantDto(
                id = tenant.id,
                name = tenant.name,
                type = tenant.type,
                createdAt = tenant.createdAt,
                isActive = !tenant.deleted
            )
        }
    }
}

// Additional DTO for admin operations
data class TenantDetailDto(
    val id: UUID,
    val name: String,
    val type: String,
    val createdAt: Instant,
    val createdBy: String,
    val updatedAt: Instant?,
    val updatedBy: String?,
    val isActive: Boolean,
    val userCount: Long? = null
) {
    companion object {
        fun from(tenant: Tenant, userCount: Long? = null): TenantDetailDto {
            return TenantDetailDto(
                id = tenant.id,
                name = tenant.name,
                type = tenant.type,
                createdAt = tenant.createdAt,
                createdBy = tenant.createdBy,
                updatedAt = tenant.updatedAt,
                updatedBy = tenant.updatedBy,
                isActive = !tenant.deleted,
                userCount = userCount
            )
        }
    }
}