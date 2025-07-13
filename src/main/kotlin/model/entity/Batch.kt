package com.muditsahni.model.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

/**
    * Batch entity representing a collection of records.
    * It includes metadata such as status, type, ownership, and timestamps.
    * The `Batch` class is used to manage and track batches of records in the system.
    * It is stored in a MongoDB collection named "batches".
    * @Document(collection = "batches")
    * @property id Unique identifier for the batch.
    * @property tenantName Identifier/Name for the tenant to which the batch belongs.
    * @property name Name of the batch.
    * @property status Current status of the batch (e.g., active, completed).
    * @property type Type of the batch (e.g., import, export).
    * @property records List of record IDs associated with the batch.
    * @property createdAt Timestamp when the batch was created.
    * @property updatedAt Timestamp when the batch was last updated.
    * @property createdBy User who created the batch.
    * @property updatedBy User who last updated the batch.
    * @property owners List of users who own the batch.
    * @property editors List of users who can edit the batch.
    * @property viewers List of users who can view the batch.
    * @property tags Map of additional metadata tags associated with the batch.
    * @property deleted Flag indicating whether the batch is deleted or not.
 */
@Document(collection = "batches")
data class Batch(
    @Id
    val id: UUID = UUID.randomUUID(),
    val tenantName: String,
    val name: String,
    var status: BatchStatus,
    val type: BatchType,
    val records: List<UUID> = emptyList(),
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null,
    val createdBy: String,
    var updatedBy: String? = null,
    val owners: List<String> = mutableListOf<String>(),
    val editors: List<String> = mutableListOf<String>(),
    val viewers: List<String> = mutableListOf<String>(),
    val tags: Map<String, String> = emptyMap(),
    var deleted : Boolean = false
)