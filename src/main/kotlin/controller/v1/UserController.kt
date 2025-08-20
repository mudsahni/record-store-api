package com.muditsahni.controller.v1

import com.muditsahni.model.dto.request.CreateUserRequestDto
import com.muditsahni.model.dto.response.UserResponseDto
import com.muditsahni.model.dto.response.toUserResponseDto
import com.muditsahni.model.entity.Role
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.security.dto.request.ChangePasswordRequest
import com.muditsahni.service.v1.DefaultUserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "Users", description = "Create & lookup users")
@Validated
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: DefaultUserService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Handles the retrieval of a user by their email or phone number.
     * This endpoint allows users to retrieve user information based on the provided email or phone.
     * @param email The email of the user to retrieve (optional).
     * @param phoneNumber The phone number of the user to retrieve (optional).
     * @return A response containing the user's information or an error if the user is not found.
     */
    @Operation(
        summary = "Get user by email or phone",
        description = "Takes email or phone number, returns the user details"
    )
    @ApiResponse(responseCode = "200", description = "User retrieved successfully")
    @GetMapping()
    suspend fun getUserByQueryParams(
        @PathVariable tenantName: String,
        @RequestParam("email") email: String?,
        @RequestParam("phoneNumber") phoneNumber: String?,
    ): ResponseEntity<UserResponseDto> {

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (currentTenant.name != tenantName) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (email == null && phoneNumber == null) {
            return ResponseEntity.badRequest().build()
        }

        logger.info {
            "Received request to get user by email or phone number: " +
                    "email=$email, phoneNumber=$phoneNumber, tenant=$tenantName"
        }

        try {
            val user = if (email != null) {
                userService.getUserByEmail(email, tenantName)
            } else {
                userService.getUserByPhoneNumber(phoneNumber!!, tenantName)
            }

            return if (user != null) {
                ResponseEntity.ok(user.toUserResponseDto())
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving user: ${e.message}" }
            throw e
        }
    }

    /**
     * Handles the deactivation of a user.
     * This endpoint allows users to deactivate a user by providing either an email or a phone number.
     * @param email The email of the user to deactivate (optional).
     * @param phoneNumber The phone number of the user to deactivate (optional).
     * @return A response indicating the success or failure of the deactivation operation.
     */
    @Operation(
        summary = "Deactivate user",
        description = "Takes email or phone number, deactivates the user"
    )
    @ApiResponse(responseCode = "200", description = "User deactivated successfully")
    @DeleteMapping
    suspend fun deactivateUser(
        @RequestParam("email") email: String?,
        @RequestParam("phoneNumber") phoneNumber: String?,
    ): ResponseEntity<String> {

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        // Check if current user has permission to deactivate users
        if (!CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (email == null && phoneNumber == null) {
            return ResponseEntity.badRequest().body("Either email or phone number must be provided")
        }

        logger.info {
            "Received request to deactivate user: " +
                    "email=$email, phoneNumber=$phoneNumber, tenant=${currentTenant.name}"
        }

        return try {
            val success = if (email != null) {
                userService.deactivateUserByEmail(email, currentTenant.name)
            } else {
                userService.deactivateUserByPhoneNumber(phoneNumber!!, currentTenant.name)
            }

            if (success) {
                ResponseEntity.ok("User deactivated successfully")
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deactivating user: ${e.message}" }
            throw e
        }
    }

    /**
     * Get current user information.
     */
    @Operation(
        summary = "Get current user",
        description = "Returns the currently authenticated user's information"
    )
    @ApiResponse(responseCode = "200", description = "Current user retrieved successfully")
    @GetMapping("/me")
    suspend fun getCurrentUser(): ResponseEntity<UserResponseDto> {
        val currentUser = CoroutineSecurityUtils.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(currentUser.toUserResponseDto())
    }

    /**
     * Change current user's password.
     */
    @Operation(
        summary = "Change password",
        description = "Changes the current user's password"
    )
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    @PostMapping("/me/change-password")
    suspend fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<String> {
        val currentUser = CoroutineSecurityUtils.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        try {
            val success = userService.changePassword(
                email = currentUser.email,
                tenantName = currentTenant.name,
                oldPassword = request.oldPassword,
                newPassword = request.newPassword
            )

            return if (success) {
                ResponseEntity.ok("Password changed successfully")
            } else {
                ResponseEntity.badRequest().body("Invalid old password")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error changing password: ${e.message}" }
            throw e
        }
    }
}
