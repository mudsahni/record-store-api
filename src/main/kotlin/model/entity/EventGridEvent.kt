package com.muditsahni.model.entity

import java.time.OffsetDateTime

/**
 * Data class representing an Event Grid event
 * @property id The unique identifier for the event.
 * @property eventType The type of the event.
 * @property subject The subject of the event.
 * @property eventTime The timestamp when the event occurred.
 * @property data The data payload of the event.
 * @property dataVersion The version of the data schema.
 * @property metadataVersion The version of the metadata schema.
 * @property topic The topic to which the event belongs.
 */
data class EventGridEvent(
    val id: String,
    val eventType: String,
    val subject: String,
    val eventTime: OffsetDateTime,
    val data: Any,
    val dataVersion: String,
    val metadataVersion: String,
    val topic: String
)

/**
 * Data class representing the payload for a Blob Created event
 * @property api The API used to create the blob.
 * @property clientRequestId The client request ID.
 * @property requestId The server request ID.
 * @property eTag The ETag of the created blob.
 * @property contentType The content type of the blob.
 * @property contentLength The size of the blob in bytes.
 * @property blobType The type of the blob (e.g., BlockBlob).
 * @property url The URL of the created blob.
 * @property sequencer The sequencer value for the blob.
 * @property storageDiagnostics Diagnostics information related to storage operations.
 */
data class BlobCreatedEventData(
    val api: String,
    val clientRequestId: String,
    val requestId: String,
    val eTag: String,
    val contentType: String,
    val contentLength: Long,
    val blobType: String,
    val url: String,
    val sequencer: String,
    val storageDiagnostics: Map<String, Any>
)

/**
 * Data class representing the payload for a Blob Deleted event
 * @property api The API used to delete the blob.
 * @property clientRequestId The client request ID.
 * @property requestId The server request ID.
 * @property contentType The content type of the blob.
 * @property blobType The type of the blob (e.g., BlockBlob).
 * @property url The URL of the deleted blob.
 * @property sequencer The sequencer value for the blob.
 * @property storageDiagnostics Diagnostics information related to storage operations.
 */
data class BlobDeletedEventData(
    val api: String,
    val clientRequestId: String,
    val requestId: String,
    val contentType: String,
    val blobType: String,
    val url: String,
    val sequencer: String,
    val storageDiagnostics: Map<String, Any>
)