package com.muditsahni.model.entity

/**
 * Data class containing upload information
 * @property uploadUrl The URL to which the file should be uploaded.
 * @property blobName The name of the blob in the storage system.
 * @property uploadSessionId A unique identifier for the upload session.
 * @property metadata A map of metadata key-value pairs associated with the upload.
 */
data class UploadInfo(
    val uploadUrl: String,
    val blobName: String,
    val uploadSessionId: String,
    val metadata: Map<String, String>
)