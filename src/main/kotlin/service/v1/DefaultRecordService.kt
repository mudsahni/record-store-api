package com.muditsahni.service.v1

import mu.KotlinLogging
import com.muditsahni.model.entity.Record
import com.muditsahni.model.entity.RecordStatus
import com.muditsahni.repository.RecordRepository
import com.muditsahni.service.AzureBlobStorageService
import com.muditsahni.service.command.record.UploadRequestCommand
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import com.muditsahni.error.RecordNotFoundException
import com.muditsahni.error.StorageServiceException
import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.FileType
import com.muditsahni.model.entity.RecordType
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.UploadInfo
import com.muditsahni.model.entity.User
import com.muditsahni.service.RecordService
import com.muditsahni.service.command.record.UploadRequestResult

@Service
class DefaultRecordService(
    override val recordRepository: RecordRepository,
    override val storageService: AzureBlobStorageService
): RecordService {

    private val logger = KotlinLogging.logger {}

    override suspend fun createRecord(
        tenant: Tenant,
        batchId: UUID,
        type: RecordType,
        fileType: FileType,
        fileName: String,
        user: User
    ): Record {
        val record = Record(
            id = UUID.randomUUID(),
            batchId = batchId,
            tenantName = tenant.name,
            type = type,
            fileType = fileType,
            status = RecordStatus.CREATED,
            createdBy = user.email,
            fileName = fileName,
        )
        return recordRepository.save(record)
    }

    override suspend fun requestUpload(
        tenantName: String,
        recordId: UUID,
        fileName: String
    ): UploadInfo {
        val uploadUrl = storageService.generateUploadUrl(savedRecord.id, savedRecord.fileName)
        return UploadRequestResult(savedRecord.id, uploadUrl)
    }

    override suspend fun completeUpload(
        tenantName: String,
        recordId: UUID,
    ) {
        val record = recordRepository.findById(recordId)
            ?: throw RecordNotFoundException("Record not found with ID: $recordId")

        // Verify the blob actually exists in storage
        val blobExists = storageService.blobExists(recordId, record.fileName)
        if (!blobExists) {
            throw StorageServiceException("File not found in storage. Upload may not have completed.")
        }

        record.status = RecordStatus.UPLOADED
        record.updatedAt = Instant.now()
        record.updatedBy = "system"
        recordRepository.save(record)

        logger.info { "Record $recordId marked as UPLOADED" }

    }
}