package com.muditsahni.security.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Data Transfer Object (DTO) for changing a user's password.
 */
data class ChangePasswordRequest(
    @field:NotBlank(message = "Old password must not be blank")
    @JsonProperty("old_password")
    val oldPassword: String,

    @field:NotBlank(message = "New password must not be blank")
    @field:Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+\$",
        message = "New password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @JsonProperty("new_password")
    val newPassword: String
)