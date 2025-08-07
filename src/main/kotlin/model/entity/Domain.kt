package com.muditsahni.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

/**
 * Represents a domain in the system.
 * This class is used to store domain information in the MongoDB database.
 * @Document(collection = "domains")
 * @property id Unique identifier for the domain.
 * @property name Name of the domain, must be unique.
 * @property tenantName Identifier of the tenant to which this domain belongs.
 * @property createdAt Timestamp when the domain was created.
 * @property createdBy Identifier of the user who created this domain.
 * @property deleted Flag indicating whether the domain is deleted or not.
 * @property updatedBy Identifier of the user who last updated this domain, optional.
 * @property updatedAt Timestamp when the domain was last updated, optional.
 */
@Document(collection = "domains")
data class Domain(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Indexed(unique = true)
    val name: String,
    val tenantName: String,
    val createdAt: Instant,
    val createdBy: String,
    val deleted: Boolean = false,
    val updatedBy: String? = null,
    val updatedAt: Instant? = null
)