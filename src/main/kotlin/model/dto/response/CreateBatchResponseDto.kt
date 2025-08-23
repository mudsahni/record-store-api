package com.muditsahni.model.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import java.time.Instant
import java.util.UUID

data class CreateBatchResponseDto(
    @JsonProperty("_id")
    val id: UUID,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("status")
    val status: BatchStatus,
    @JsonProperty("type")
    val type: BatchType,
    @JsonProperty("tags")
    val tags: Map<String, String>? = null,
    @JsonProperty("records")
    val records: List<UUID>,
    @JsonProperty("created_at")
    val createdAt: Instant,
    @JsonProperty("created_by")
    val createdBy: String
)

fun Batch.toCreateBatchResponseDto(): CreateBatchResponseDto {
    return CreateBatchResponseDto(
        id = id,
        name = name,
        status = status,
        type = type,
        tags = tags,
        records = records,
        createdAt = createdAt,
        createdBy = createdBy
    )
}