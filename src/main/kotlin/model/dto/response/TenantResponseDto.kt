package com.muditsahni.model.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.muditsahni.model.entity.Tenant
import java.util.UUID

/**
 * Data Transfer Object for the response of creating a tenant.
 * This DTO is used to encapsulate the data returned after a tenant is created.
 * @param id The unique identifier of the tenant, represented as a uuid.
 * @param name The name of the tenant.
 * @param type The type of the tenant.
 * @param createdAt The timestamp when the tenant was created, formatted as a string.
 */
data class TenantResponseDto(
    @JsonProperty("_id")
    val id: UUID,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("created_at")
    val createdAt: String,
)

fun Tenant.toTenantResponseDto(): TenantResponseDto {
    return TenantResponseDto(
        id = id,
        name = name,
        type = type,
        createdAt = createdAt.toString()
    )
}
