package com.muditsahni.model.dto.response

import com.muditsahni.model.entity.FileType
import com.muditsahni.model.entity.Record
import com.muditsahni.model.entity.RecordStatus
import com.muditsahni.model.entity.RecordType
import com.muditsahni.model.entity.UploadInfo
import java.time.Instant
import java.util.UUID


/**
 * Data Transfer Object for creating a record response.
 *
 * @property id The unique identifier of the record.
 * @property batchId The identifier of the batch to which the record belongs.
 * @property status The current status of the record.
 * @property type The type of the record (e.g., IMAGE, DOCUMENT).
 * @property fileType The file type of the record (e.g., PDF, JPEG).
 * @property uploadInfo Information related to the upload process, including URL and expiration.
 * @property createdAt The timestamp when the record was created.
 * @property createdBy The user who created the record.
 */
data class CreateRecordResponseDto (
    val id: UUID,
    val batchId: UUID,
    val status: RecordStatus,
    val type: RecordType,
    val fileType: FileType,
    val uploadInfo: UploadInfo,
    val createdAt: Instant,
    val createdBy: String
)


fun Record.toCreateRecordResponseDto(uploadInfo: UploadInfo): CreateRecordResponseDto {
    return CreateRecordResponseDto(
        id = id,
        batchId = batchId,
        status = status,
        type = type,
        fileType = fileType,
        uploadInfo = uploadInfo,
        createdAt = createdAt,
        createdBy = createdBy
    )
}