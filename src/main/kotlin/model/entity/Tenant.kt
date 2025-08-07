package com.muditsahni.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

/**
 * Represents a tenant in the system.
 * This class is used to store tenant information in the MongoDB database.
 * @Document(collection = "tenants")
 * @property id Unique identifier for the tenant.
 * @property name Name of the tenant, must be unique.
 * @property domains Domains associated with the tenant, must be unique.
 * @property type Type of the tenant (e.g., organization, individual).
 * @property createdAt Timestamp when the tenant was created.
 * @property updatedAt Timestamp when the tenant was last updated, optional.
 * @property deleted Flag indicating whether the tenant is deleted or not.
 * @property createdBy Identifier of the user who created this tenant.
 */
@Document(collection = "tenants")
data class Tenant(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Indexed(unique = true)
    val name: String,
    var domains: Set<String>,
    var type: String,
    val createdAt: Instant = Instant.now(),
    val createdBy: String,
    var updatedAt: Instant? = null,
    var deleted: Boolean = false,
    var updatedBy: String? = null
)
