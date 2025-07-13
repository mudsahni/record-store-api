package com.muditsahni.model.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.muditsahni.model.entity.BatchType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Data Transfer Object for creating a batch request.
 * This DTO is used to encapsulate the data required to create a new batch.
 * @param name The name of the batch.
 * @param type The type of the batch, represented by [BatchType].
 * @param tags Optional tags associated with the batch, represented as a map of key-value pairs.
 */
data class CreateBatchRequestDto(
    @field:NotBlank(message = "name must not be blank")
    @field:Size(min = 4, max = 50, message = "name must be between 3 and 50 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotNull(message = "type must not be null")
    @JsonProperty("type")
    val type: BatchType,

    @field:Size(max = 10, message = "you can specify at most 10 tags")
    @JsonProperty("tags")
    val tags: Map<@NotBlank(message = "tag keys must not be blank") String,
            @NotBlank(message = "tag values must not be blank") String> = emptyMap()
)