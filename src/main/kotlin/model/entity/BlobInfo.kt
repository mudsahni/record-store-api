package com.muditsahni.model.entity

import java.time.OffsetDateTime

/**
 * Data class containing blob information
 * @property blobName The name of the blob in the storage system.
 * @property metadata A map of metadata key-value pairs associated with the blob.
 * @property createdOn The timestamp when the blob was created.
 * @property size The size of the blob in bytes.
 */
data class BlobInfo(
    val blobName: String,
    val metadata: Map<String, String>,
    val createdOn: OffsetDateTime?,
    val size: Long
)