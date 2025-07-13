//package com.muditsahni.service.v1
//
//import mu.KotlinLogging
//import com.muditsahni.models.entity.Record
//import com.muditsahni.models.entity.RecordStatus
//import com.muditsahni.repository.RecordRepository
//import com.muditsahni.service.AzureBlobStorageService
//import com.muditsahni.service.command.UploadRequestCommand
//import com.muditsahni.service.command.UploadRequestResult
//import org.springframework.stereotype.Service
//import java.time.Instant
//import java.util.UUID
//import com.muditsahni.error.RecordNotFoundException
//import com.muditsahni.error.StorageServiceException
//
//@Service
//class DefaultRecordService(
//    override val recordRepository: RecordRepository,
//    override val storageService: AzureBlobStorageService
//): RecordService {
//
//    private val logger = KotlinLogging.logger {}
//
//    override suspend fun requestUpload(
//        tenantId: String,
//        uploadRequestCommand: UploadRequestCommand
//    ): UploadRequestResult {
//        val record = Record(
//            batchId = uploadRequestCommand.batchId,
//            fileName = uploadRequestCommand.fileName,
//            type = uploadRequestCommand.type,
//            createdBy = uploadRequestCommand.createdBy,
//            status = RecordStatus.CREATED
//        )
//        val savedRecord = recordRepository.save(record)
//        val uploadUrl = storageService.generateUploadUrl(savedRecord.id, savedRecord.fileName)
//        return UploadRequestResult(savedRecord.id, uploadUrl)
//    }
//
//    override suspend fun completeUpload(
//        tenantId: String,
//        recordId: UUID,
//    ) {
//        val record = recordRepository.findById(recordId)
//            ?: throw RecordNotFoundException("Record not found with ID: $recordId")
//
//        // Verify the blob actually exists in storage
//        val blobExists = storageService.blobExists(recordId, record.fileName)
//        if (!blobExists) {
//            throw StorageServiceException("File not found in storage. Upload may not have completed.")
//        }
//
//        record.status = RecordStatus.UPLOADED
//        record.updatedAt = Instant.now()
//        record.updatedBy = "system"
//        recordRepository.save(record)
//
//        logger.info { "Record $recordId marked as UPLOADED" }
//
//    }
//}