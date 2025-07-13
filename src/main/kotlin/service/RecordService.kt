package com.muditsahni.service.v1

import com.muditsahni.service.command.UploadRequestCommand
import com.muditsahni.repository.RecordRepository
import com.muditsahni.service.AzureBlobStorageService
import com.muditsahni.service.command.UploadRequestResult
import java.util.UUID

interface RecordService {

    val recordRepository: RecordRepository
    val storageService: AzureBlobStorageService

    suspend fun requestUpload(
        tenantId: String,
        uploadRequestCommand: UploadRequestCommand
    ): UploadRequestResult

    suspend fun completeUpload(
        tenantId: String,
        recordId: UUID,
    )
}