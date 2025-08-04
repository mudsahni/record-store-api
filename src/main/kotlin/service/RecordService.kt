package com.muditsahni.service

import com.muditsahni.service.command.record.UploadRequestCommand
import com.muditsahni.repository.RecordRepository
import com.muditsahni.service.command.record.UploadRequestResult
import java.util.UUID

interface RecordService {

    val recordRepository: RecordRepository
    val storageService: AzureBlobStorageService

    suspend fun requestUpload(
        tenantName: String,
        uploadRequestCommand: UploadRequestCommand
    ): UploadRequestResult

    suspend fun completeUpload(
        tenantName: String,
        recordId: UUID,
    )
}