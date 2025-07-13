package com.muditsahni.model.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Data Transfer Object (DTO) for creating a new user.
 * This DTO is used to encapsulate the data required
 * to create a new user in the system.
 * @property email The email address of the user.
 * @property phoneNumber The phone number of the user.
 * @property password The password for the user.
 * @property firstName The first name of the user (optional).
 * @property lastName The last name of the user (optional).
 */
data class CreateUserRequestDto(
    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email must be a valid address")
    @JsonProperty("email")
    val email: String,

    @field:NotBlank(message = "Phone number must not be blank")
    // very basic international‐style: allows an optional leading '+' and 10–15 digits
    @field:Pattern(
        regexp = "^\\+?[0-9]{10,15}\$",
        message = "Phone number must be 10–15 digits, optionally starting with '+'"
    )
    @JsonProperty("phone_number")
    val phoneNumber: String,

    @field:NotBlank(message = "Password must not be blank")
    @field:Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+\$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @JsonProperty("password")
    val password: String,

    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @JsonProperty("first_name")
    val firstName: String? = null,

    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @JsonProperty("last_name")
    val lastName: String? = null
)