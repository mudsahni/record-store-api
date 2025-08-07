package com.muditsahni.model.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Data Transfer Object for creating a tenant request.
 * This DTO is used to encapsulate the data required to create a new tenant.
 * @param name The name of the tenant.
 * @param type The type of the tenant.
 * @param domains A set of domains associated with the tenant.
 */
data class CreateTenantRequestDto(
    @field:NotBlank(message = "name must not be blank")
    @field:Size(min = 4, max = 50, message = "name must be between 3 and 50 characters")
    @JsonProperty("name")
    val name: String,

    @field:NotBlank(message = "type must not be blank")
    @field:Size(min = 4, max = 50, message = "name must be between 3 and 50 characters")
    @JsonProperty("type")
    val type: String,
    @JsonProperty("domains")
    @field:Size(min = 1, max = 4, message = "you can at most specify 4 domains")
    val domains: Set<String>
)