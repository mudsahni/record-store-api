package com.muditsahni.models.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.muditsahni.model.entity.RecordType
import com.muditsahni.service.command.record.UploadRequestCommand
import java.util.UUID

data class UploadRequestDto(
    @JsonProperty("batch_id")
    val batchId: UUID,

    @JsonProperty("file_name")
    val fileName: String,

    @JsonProperty("type")
    val type: RecordType,

    @JsonProperty("created_by")
    val createdBy: String
)

fun UploadRequestDto.toUploadRequestCommand(): UploadRequestCommand {
    return UploadRequestCommand(
        batchId = batchId,
        fileName = fileName,
        type = type,
        createdBy = createdBy
    )
}