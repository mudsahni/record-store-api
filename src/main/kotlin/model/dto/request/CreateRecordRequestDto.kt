package com.muditsahni.model.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.muditsahni.model.entity.FileType
import com.muditsahni.model.entity.RecordType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * Data Transfer Object for creating a record request.
 * This DTO is used to encapsulate the data required to create a new record.
 * @param batchId The ID of the batch to which the record belongs.
 * @param name The name of the record.
 * @param type The type of the record, represented by [RecordType].
 * @param fileType The file type of the record, represented by [FileType].
 */
data class CreateRecordRequestDto(

    @field:NotBlank("batch id must not be blank")
    @field:Size(min = 1, max = 50, message = "batch id must be between 1 and 50 characters")
    @field:JsonProperty("batchId")
    val batchId: UUID,

    @field:NotBlank(message = "name must not be blank")
    @field:Size(min = 4, max = 50, message = "name must be between 3 and 50 characters")
    @field:JsonProperty("name")
    val name: String,

    @field:NotNull(message = "type must not be null")
    @field:JsonProperty("type")
    val type: RecordType,

    @field:NotNull(message = "fileType must not be null")
    @field:JsonProperty("fileType")
    val fileType: FileType
)