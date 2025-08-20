package com.muditsahni.service

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.azure.storage.common.sas.SasProtocol
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AzureBlobStorageService(
    @Value("\${azure.storage.connection-string}") private val connectionString: String,
    @Value("\${azure.storage.container-name}") private val containerName: String
) {

    private val blobServiceClient: BlobServiceClient = BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient()

    private val containerClient = blobServiceClient.getBlobContainerClient(containerName)

    /**
     * Generates a pre-signed URL for uploading a file
     *
     * @param recordId The ID of the record
     * @param fileName Original file name (for determining content type)
     * @return Upload URL with write permissions that expires in 15 minutes
     */
    fun generateUploadUrl(recordId: UUID, fileName: String): String {
        val blobName = "records/${recordId}/${fileName.sanitizeFileName()}"
        val blobClient = containerClient.getBlobClient(blobName)

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
        return "${blobClient.blobUrl}?${sasToken}"
    }

    /**
     * Generates a pre-signed URL for downloading a file
     *
     * @param recordId The ID of the record
     * @param fileName Original file name
     * @return Download URL with read permissions that expires in 60 minutes
     */
    fun generateDownloadUrl(recordId: UUID, fileName: String): String {
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
     * Helper method to sanitize file names for Azure Blob Storage
     */
    private fun String.sanitizeFileName(): String {
        return this.replace(Regex("[^a-zA-Z0-9.-]"), "_")
    }

    /**
     * Checks if a blob exists in the storage
     *
     * @param recordId The ID of the record
     * @param fileName The filename
     * @return true if the blob exists, false otherwise
     */
    fun blobExists(recordId: UUID, fileName: String): Boolean {
        val blobName = "records/${recordId}/${fileName.sanitizeFileName()}"
        val blobClient = containerClient.getBlobClient(blobName)
        return blobClient.exists()
    }

    /**
     * Deletes a blob from the storage
     *
     * @param recordId The ID of the record
     * @param fileName The filename
     */
    fun deleteBlob(recordId: UUID, fileName: String) {
        val blobName = "records/${recordId}/${fileName.sanitizeFileName()}"
        val blobClient = containerClient.getBlobClient(blobName)
        blobClient.deleteIfExists()
    }
}