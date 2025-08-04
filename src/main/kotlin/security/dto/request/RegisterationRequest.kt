package com.muditsahni.security.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Data Transfer Object (DTO) for user registration.
 * This DTO is used to encapsulate the data required
 * to register a new user in the system.
 * @property firstName The first name of the user.
 * @property lastName The last name of the user.
 * @property email The email address of the user.
 * @property phoneNumber The phone number of the user.
 * @property password The password for the user.
 */
data class RegisterationRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 50)
    @JsonProperty("first_name")
    val firstName: String,
    @field:NotBlank
    @field:Size(min = 2, max = 50)
    @JsonProperty("last_name")
    val lastName: String,
    @field:NotBlank
    @field:Email
    @JsonProperty("email")
    val email: String,
    @field:NotBlank
    @field:Size(min = 10, max = 15)
    @JsonProperty("phone_number")
    val phoneNumber: String,
    @field:NotBlank @field:Size(min = 8)
    @JsonProperty("password")
    val password: String
)
