package com.muditsahni.model.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus

/**
 * Data Transfer Object (DTO) for user response.
 * This DTO is used to encapsulate the data returned
 * when retrieving user information.
 * @property email The email address of the user.
 * @property tenantName The name of the tenant to which the user belongs.
 * @property phoneNumber The phone number of the user.
 * @property firstName The first name of the user (optional).
 * @property lastName The last name of the user (optional).
 * @property createdAt The timestamp when the user was created.
 * @property updatedAt The timestamp when the user was last updated.
 * @property status Indicates user status.
 */
data class UserResponseDto(
    @JsonProperty("email")
    val email: String,
    @JsonProperty("tenant_name")
    val tenantName: String,
    @JsonProperty("phone_number")
    val phoneNumber: String,
    @JsonProperty("first_name")
    val firstName: String? = null,
    @JsonProperty("last_name")
    val lastName: String? = null,
    @JsonProperty("created_at")
    val createdAt: String,
    @JsonProperty("created_by")
    val createdBy: String,
    @JsonProperty("updated_at")
    val updatedAt: String,
    @JsonProperty("updated_by")
    val updatedBy: String? = null,
    @JsonProperty("status")
    val status: UserStatus,
)

/**
 * Extension function to convert a User entity to a UserResponseDto.
 * This function maps the properties of the User entity to the corresponding
 * properties in the UserResponseDto.
 * @return A UserResponseDto containing the user's information.
 */
fun User.toUserResponseDto(): UserResponseDto {
    return UserResponseDto(
        email = email,
        tenantName = tenantName,
        phoneNumber = phoneNumber,
        firstName = firstName,
        lastName = lastName,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        status = status,
        createdBy = createdBy,
        updatedBy = updatedBy
    )
}