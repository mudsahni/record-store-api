package com.muditsahni.service

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.azure.storage.common.sas.SasProtocol
import com.muditsahni.model.entity.BlobInfo
import com.muditsahni.model.entity.UploadInfo
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AzureBlobStorageService(
    @Value("\${azure.storage.connection-string}") private val connectionString: String,
    @Value("\${azure.storage.container-prefix:tenant}") private val containerPrefix: String
) {
    private val logger = KotlinLogging.logger {}

    private val blobServiceClient: BlobServiceClient = BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient()

    /**
     * Gets or creates a container client for a specific tenant
     * @param tenantId The ID of the tenant
     * @return BlobContainerClient for the tenant
     * @throws IllegalArgumentException if tenantId is invalid
     */
    private fun getContainerClient(tenantId: String): com.azure.storage.blob.BlobContainerClient {
        val containerName = "${containerPrefix}-${tenantId.lowercase()}"
        val containerClient = blobServiceClient.getBlobContainerClient(containerName)

        // Create container if it doesn't exist
        if (!containerClient.exists()) {
            containerClient.create()
            logger.info("Created new container for tenant: $containerName")
        }

        return containerClient
    }

    /**
     * Generates a pre-signed URL for uploading a file
     * @param tenantName The name of the tenant
     * @param recordId The ID of the record
     * @param fileName Original file name (for determining content type)
     * @param uploadSessionId Optional session ID to track multiple uploads
     * @param additionalMetadata Optional additional metadata for tracking
     * @return UploadInfo containing URL and blob details
     */
    fun generateUploadUrl(
        tenantName: String,
        recordId: UUID,
        fileName: String,
        uploadSessionId: String = UUID.randomUUID().toString(),
        additionalMetadata: Map<String, String> = emptyMap()
    ): UploadInfo {
        val containerClient = getContainerClient(tenantName)

        val timestamp = System.currentTimeMillis()
        val blobName = "records/${recordId}/${timestamp}_${fileName.sanitizeFileName()}"
        val blobClient = containerClient.getBlobClient(blobName)

        // Prepare metadata for Event Grid correlation
        val metadata = mutableMapOf(
            "recordId" to recordId.toString(),
            "originalFileName" to fileName,
            "uploadSessionId" to uploadSessionId,
            "uploadTimestamp" to timestamp.toString(),
            "status" to "pending"
        ).apply {
            putAll(additionalMetadata)
        }

        // Set blob metadata (will be included in Event Grid events)
        try {
            blobClient.setMetadata(metadata)
        } catch (e: Exception) {
            // Metadata will be set during upload if blob doesn't exist yet
            // This is normal for pre-signed upload URLs
        }

        // Define SAS token permissions and expiry
        val permissions = BlobSasPermission()
            .setWritePermission(true)
            .setCreatePermission(true) // Need create permission for new blobs


        val expiryTime = OffsetDateTime.now().plusMinutes(15)

        // Create SAS token with specified permissions
        val sasSignatureValues = BlobServiceSasSignatureValues(expiryTime, permissions)
            .setProtocol(SasProtocol.HTTPS_ONLY)

        // Generate the SAS token
        val sasToken = blobClient.generateSas(sasSignatureValues)

        // Construct the full URL
        val uploadUrl = "${blobClient.blobUrl}?${sasToken}"

        return UploadInfo(
            uploadUrl = uploadUrl,
            blobName = blobName,
            uploadSessionId = uploadSessionId,
            metadata = metadata
        )
    }

    /**
     * Generates multiple upload URLs for batch upload
     * @param tenantName The name of the tenant
     * @param recordId The ID of the record
     * @param fileNames List of original file names
     * @param uploadSessionId Optional session ID to track multiple uploads
     * @param additionalMetadata Optional additional metadata for tracking
     * @return List of UploadInfo containing URLs and blob details
     */
    fun generateMultipleUploadUrls(
        tenantName: String,
        recordId: UUID,
        fileNames: List<String>,
        uploadSessionId: String = UUID.randomUUID().toString(),
        additionalMetadata: Map<String, String> = emptyMap()
    ): List<UploadInfo> {
        return fileNames.map { fileName ->
            generateUploadUrl(tenantName, recordId, fileName, uploadSessionId, additionalMetadata)
        }
    }

    /**
     * Generates a pre-signed URL for downloading a file
     *
     * @param tenantName The name of the tenant
     * @param recordId The ID of the record
     * @param fileName Original file name
     * @return Download URL with read permissions that expires in 60 minutes
     */
    fun generateDownloadUrl(tenantName: String, recordId: UUID, fileName: String): String {
        val containerClient = getContainerClient(tenantName)
        val blobName = "records/${recordId}/${fileName.sanitizeFileName()}"
        val blobClient = containerClient.getBlobClient(blobName)

        // Define SAS token permissions and expiry
        val permissions = BlobSasPermission()
            .setReadPermission(true)

        val expiryTime = OffsetDateTime.now().plusHours(1)

        // Create SAS token with specified permissions
        val sasSignatureValues = BlobServiceSasSignatureValues(expiryTime, permissions)
            .setProtocol(SasProtocol.HTTPS_ONLY)

        // Generate the SAS token
        val sasToken = blobClient.generateSas(sasSignatureValues)

        // Construct the full URL
        return "${blobClient.blobUrl}?${sasToken}"
    }

    /**
     * Updates blob metadata (useful for status updates)
     * @param tenantName The name of the tenant
     * @param blobName The name of the blob
     * @param metadata Key-value pairs to update in metadata
     */
    fun updateBlobMetadata(tenantName: String, blobName: String, metadata: Map<String, String>) {
        val containerClient = getContainerClient(tenantName)
        val blobClient = containerClient.getBlobClient(blobName)
        if (blobClient.exists()) {
            val existingMetadata = blobClient.properties.metadata.toMutableMap()
            existingMetadata.putAll(metadata)
            blobClient.setMetadata(existingMetadata)
        }
    }

    /**
     * Retrieves blob metadata
     * @param tenantName The name of the tenant
     * @param blobName The name of the blob
     * @return Metadata map or null if blob doesn't exist
     */
    fun getBlobMetadata(tenantName: String, blobName: String): Map<String, String>? {
        val containerClient = getContainerClient(tenantName)
        val blobClient = containerClient.getBlobClient(blobName)
        return if (blobClient.exists()) {
            blobClient.properties.metadata
        } else {
            null
        }
    }

    /**
     * Lists all blobs for a record with their metadata
     * @param tenantName The name of the tenant
     * @param recordId The ID of the record
     * @return List of BlobInfo containing blob details and metadata
     */
    fun listBlobsForRecord(tenantName: String, recordId: UUID): List<BlobInfo> {
        val containerClient = getContainerClient(tenantName)
        val prefix = "records/${recordId}/"
        return containerClient.listBlobsByHierarchy(prefix).map { blobItem ->
            val blobClient = containerClient.getBlobClient(blobItem.name)
            val properties = blobClient.properties
            BlobInfo(
                blobName = blobItem.name,
                metadata = properties.metadata,
                createdOn = properties.creationTime,
                size = properties.blobSize
            )
        }.toList()
    }

    /**
     * Helper method to sanitize file names for Azure Blob Storage
     * Replaces invalid characters with underscores
     * @receiver Original file name
     * @return Sanitized file name
     */
    private fun String.sanitizeFileName(): String {
        return this.replace(Regex("[^a-zA-Z0-9.-]"), "_")
    }

    /**
     * Checks if a blob exists in the storage
     *
     * @param tenantName The name of the tenant
     * @param recordId The ID of the record
     * @param fileName The filename
     * @return true if the blob exists, false otherwise
     */
    fun blobExists(tenantName: String, recordId: UUID, fileName: String): Boolean {
        val containerClient = getContainerClient(tenantName)
        val blobName = "records/${recordId}/${fileName.sanitizeFileName()}"
        val blobClient = containerClient.getBlobClient(blobName)
        return blobClient.exists()
    }

    /**
     * Checks if a blob exists by blob name
     * @param tenantName The name of the tenant
     * @param blobName The name of the blob
     * @return true if the blob exists, false otherwise
     */
    fun blobExistsByName(tenantName: String, blobName: String): Boolean {
        val containerClient = getContainerClient(tenantName)
        val blobClient = containerClient.getBlobClient(blobName)
        return blobClient.exists()
    }

    /**
     * Deletes a blob by blob name
     * @param tenantName The name of the tenant
     * @param blobName The name of the blob
     */
    fun deleteBlobByName(tenantName: String, blobName: String) {
        val containerClient = getContainerClient(tenantName)
        val blobClient = containerClient.getBlobClient(blobName)
        blobClient.deleteIfExists()
    }

    /**
     * Deletes a blob from the storage
     *
     * @param tenantName The name of the tenant
     * @param recordId The ID of the record
     * @param fileName The filename
     */
    fun deleteBlob(tenantName: String, recordId: UUID, fileName: String) {
        val containerClient = getContainerClient(tenantName)
        val blobName = "records/${recordId}/${fileName.sanitizeFileName()}"
        val blobClient = containerClient.getBlobClient(blobName)
        blobClient.deleteIfExists()
    }
}