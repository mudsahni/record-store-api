package com.muditsahni.controller.v1

import com.muditsahni.model.dto.request.CreateUserRequestDto
import com.muditsahni.model.dto.response.UserResponseDto
import com.muditsahni.model.dto.response.toUserResponseDto
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
@RequestMapping("/api/v1/tenants/{tenantName}/users")
class UserController(
    private val userService: DefaultUserService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Handles the creation of a new user.
     * This endpoint allows users to create a user by providing the necessary details
     * such as email, tenant name, phone number, and password.
     * @param createUserRequestDto The request body containing the details for creating the user.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return A response indicating the success or failure of the user creation.
     */
    @Operation(
        summary = "Create a new user",
        description = "Takes email, tenant name, phone number, password, first and last name (optional), creates the user"
    )
    @ApiResponse(responseCode = "200", description = "User created successfully")
    @PostMapping()
    suspend fun createUser(
        @PathVariable tenantName: String,
        @Valid @RequestBody createUserRequestDto: CreateUserRequestDto,
    ): ResponseEntity<UserResponseDto> {

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (currentTenant.name != tenantName) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // Check if current user has permission to create users
        if (!CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info {
            "Received user creation request: ${createUserRequestDto.email}, tenant: ${tenantName}, " +
                    "phone: ${createUserRequestDto.phoneNumber}"
        }

        try {
            val user = userService.createAuthUser(
                email = createUserRequestDto.email,
                tenantName = tenantName,
                phoneNumber = createUserRequestDto.phoneNumber,
                password = createUserRequestDto.password,
                firstName = createUserRequestDto.firstName,
                lastName = createUserRequestDto.lastName,
                roles = listOf("USER") // Default role for new users
            )
            return ResponseEntity.ok(user.toUserResponseDto())
        } catch (e: Exception) {
            logger.error(e) { "Error creating user: ${e.message}" }
            throw e
        }
    }

    /**
     * Handles the retrieval of a user by their email or phone number.
     * This endpoint allows users to retrieve user information based on the provided email or phone.
     * @param email The email of the user to retrieve (optional).
     * @param phoneNumber The phone number of the user to retrieve (optional).
     * @param tenantName The name of the tenant to which the user belongs.
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
     * @param tenantName The name of the tenant to which the user belongs.
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
        @PathVariable tenantName: String,
        @RequestParam("email") email: String?,
        @RequestParam("phoneNumber") phoneNumber: String?,
    ): ResponseEntity<String> {

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (currentTenant.name != tenantName) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // Check if current user has permission to deactivate users
        if (!CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (email == null && phoneNumber == null) {
            return ResponseEntity.badRequest().body("Either email or phone number must be provided")
        }

        logger.info {
            "Received request to deactivate user: " +
                    "email=$email, phoneNumber=$phoneNumber, tenant=$tenantName"
        }

        return try {
            val success = if (email != null) {
                userService.deactivateUserByEmail(email, tenantName)
            } else {
                userService.deactivateUserByPhoneNumber(phoneNumber!!, tenantName)
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
        @PathVariable tenantName: String,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<String> {
        val currentUser = CoroutineSecurityUtils.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (currentTenant.name != tenantName) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        try {
            val success = userService.changePassword(
                email = currentUser.email,
                tenantName = tenantName,
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