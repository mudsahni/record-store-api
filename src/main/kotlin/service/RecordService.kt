package com.muditsahni.service

import com.muditsahni.model.entity.FileType
import com.muditsahni.model.entity.Record
import com.muditsahni.model.entity.RecordType
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.service.command.record.UploadRequestCommand
import com.muditsahni.repository.RecordRepository
import com.muditsahni.service.command.record.UploadRequestResult
import java.util.UUID

interface RecordService {

    val recordRepository: RecordRepository
    val storageService: AzureBlobStorageService

    suspend fun createRecord(
        tenant: Tenant,
        batchId: UUID,
        type: RecordType,
        fileType: FileType,
        fileName: String,
        user: User
    ): Record

    suspend fun requestUpload(
        tenantName: String,
        uploadRequestCommand: UploadRequestCommand
    ): UploadRequestResult

    suspend fun completeUpload(
        tenantName: String,
        recordId: UUID,
    )
}