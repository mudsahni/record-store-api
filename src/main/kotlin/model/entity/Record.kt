package com.muditsahni.models.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

/**
 * Represents a record in the system.
 * This class is used to store information about individual records
 * associated with batches, including their status, type, and metadata.
 * @Document(collection = "records")
 * @property id Unique identifier for the record.
 * @property tenantId Identifier for the tenant to which the record belongs.
 * @property batchId Identifier for the batch to which the record belongs.
 * @property fileName Name of the file associated with the record.
 * @property filePath Optional path to the file associated with the record.
 * @property status Current status of the record (e.g., pending, processed).
 * @property type Type of the record (e.g., import, export).
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Optional timestamp when the record was last updated.
 * @property createdBy User who created the record.
 * @property updatedBy Optional user who last updated the record.
 */
@Document(collection = "records")
data class Record(
    @Id
    val id: UUID = UUID.randomUUID(),
    val tenantId: String,
    val batchId: UUID,
    val fileName: String,
    var filePath: String? = null,
    var status: RecordStatus,
    val type: RecordType,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null,
    val createdBy: String,
    var updatedBy: String? = null
)