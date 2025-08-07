package com.muditsahni.model.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.muditsahni.service.command.record.UploadRequestResult
import java.util.UUID

data class UploadRequestResponseDto(
    @JsonProperty("record_id")
    val recordId: UUID,
    @JsonProperty("upload_url")
    val uploadUrl: String? // Pre-signed URL
)

fun UploadRequestResult.toUploadRequestResponseDto(): UploadRequestResponseDto {
    return UploadRequestResponseDto(
        recordId = recordId,
        uploadUrl = uploadUrl
    )
}