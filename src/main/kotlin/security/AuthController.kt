package com.muditsahni.security

import com.muditsahni.model.entity.User
import com.muditsahni.repository.TenantRepository
import com.muditsahni.repository.UserRepository
import com.muditsahni.security.dto.request.ChangePasswordRequest
import com.muditsahni.security.dto.request.LoginRequest
import com.muditsahni.security.dto.request.RefreshTokenRequest
import com.muditsahni.security.dto.request.RegisterRequest
import com.muditsahni.security.dto.response.LoginResponse
import com.muditsahni.security.dto.response.TenantDto
import com.muditsahni.security.dto.response.UserDto
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * Handles user login.
     * This endpoint allows users to log in by providing their email and password.
     * It checks the user's credentials, handles account locking after multiple failed attempts,
     * and returns a JWT token if successful.
     * @param request The login request containing email and password.
     * @return A [ResponseEntity] containing the login response with token, user details, and tenant information,
     * or an error response if login fails.
     */
    @Operation(
        summary = "User login",
        description = "Handles user login with email and password, returns JWT token and user details"
    )
    @PostMapping("/login")
    suspend fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        try {
            val user = userRepository.findByEmail(request.email)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Invalid credentials")
                )

            // Check if account is locked
            if (user.isAccountLocked()) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Account is temporarily locked. Try again later.")
                )
            }

            // Verify password
            if (!passwordEncoder.matches(request.password, user.passwordHash)) {
                // Increment failed attempts
                val updatedUser = user.copy(
                    failedLoginAttempts = user.failedLoginAttempts + 1,
                    accountLockedUntil = if (user.shouldLockAccount()) {
                        Instant.now().plusSeconds(900) // Lock for 15 minutes
                    } else null,
                    updatedAt = Instant.now()
                )
                userRepository.save(updatedUser)

                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Invalid credentials")
                )
            }

            // Check if user is active
            if (!user.isActive) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Account is disabled")
                )
            }

            // Get tenant
            val tenant = tenantRepository.findByName(user.tenantName)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Tenant not found")
                )

            if (tenant.deleted) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Tenant is inactive")
                )
            }

            // Reset failed attempts and update last login
            val updatedUser = user.copy(
                failedLoginAttempts = 0,
                accountLockedUntil = null,
                lastLoginAt = Instant.now(),
                updatedAt = Instant.now()
            )
            userRepository.save(updatedUser)

            // Generate token
            val token = jwtService.generateToken(updatedUser, tenant)

            return ResponseEntity.ok(LoginResponse(
                token = token,
                user = UserDto.from(updatedUser),
                tenant = TenantDto.from(tenant),
                mustChangePassword = updatedUser.mustChangePassword
            ))

        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                LoginResponse(error = "Login failed: ${e.message}")
            )
        }
    }

    /**
     * Handles user registration.
     * This endpoint allows new users to register by providing their details such as name, email, phone number, password, and tenant name.
     * It validates the password strength, checks if the tenant exists and is active, and ensures the user does not already exist.
     * If successful, it creates a new user, saves it to the database, and returns a JWT token along with user and tenant details.
     * @param request The registration request containing user details and tenant information.
     * @return A [ResponseEntity] containing the login response with token, user details, and tenant information,
     * or an error response if registration fails.
     * @throws IllegalArgumentException if the password does not meet the required strength criteria.
     * @throws Exception if there is an error during registration, such as tenant not found or user already exists.
     */
    @Operation(
        summary = "User registration",
        description = "Handles user registration with details and tenant information, returns JWT token and user details"
    )
    @PostMapping("/register")
    suspend fun register(@RequestBody request: RegisterRequest): ResponseEntity<LoginResponse> {
        try {
            // Validate password strength
            if (!isPasswordValid(request.password)) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Password must be at least 8 characters with uppercase, lowercase, number, and special character")
                )
            }

            val tenant = tenantRepository.findByName(request.tenantName)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Tenant not found")
                )

            if (tenant.deleted) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Tenant is inactive")
                )
            }

            // Check if user already exists
            val existingUser = userRepository.findByEmail(request.email)
            if (existingUser != null) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "User already exists")
                )
            }

            val newUser = User(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                phoneNumber = request.phoneNumber,
                passwordHash = passwordEncoder.encode(request.password),
                tenantName = request.tenantName,
                roles = listOf("USER"),
                createdBy = "SYSTEM"
            )

            val savedUser = userRepository.save(newUser)
            val token = jwtService.generateToken(savedUser, tenant)

            return ResponseEntity.ok(LoginResponse(
                token = token,
                user = UserDto.from(savedUser),
                tenant = TenantDto.from(tenant)
            ))
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                LoginResponse(error = "Registration failed: ${e.message}")
            )
        }
    }

    @PostMapping("/change-password")
    suspend fun changePassword(@RequestBody request: ChangePasswordRequest): ResponseEntity<Unit> {
        try {
            val currentUser = CoroutineSecurityUtils.getCurrentUser()
                ?: return ResponseEntity.badRequest().build()

            // Verify current password
            if (!passwordEncoder.matches(request.oldPassword, currentUser.passwordHash)) {
                return ResponseEntity.badRequest().build()
            }

            // Validate new password
            if (!isPasswordValid(request.newPassword)) {
                return ResponseEntity.badRequest().build()
            }

            // Update password
            val updatedUser = currentUser.copy(
                passwordHash = passwordEncoder.encode(request.newPassword),
                passwordChangedAt = Instant.now(),
                mustChangePassword = false,
                updatedAt = Instant.now(),
                updatedBy = currentUser.email
            )

            userRepository.save(updatedUser)
            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/refresh")
    suspend fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<LoginResponse> {
        try {
            val refreshToken = request.refreshToken

            if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Invalid refresh token")
                )
            }

            val userId = jwtService.extractUserId(refreshToken)
            val tenantName = jwtService.extractTenantName(refreshToken)

            val user = userRepository.findById(userId)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "User not found")
                )

            val tenant = tenantRepository.findByName(tenantName)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Tenant not found")
                )

            if (!user.isActive || tenant.deleted) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Account or tenant is inactive")
                )
            }

            val newToken = jwtService.generateToken(user, tenant)
            val newRefreshToken = jwtService.generateRefreshToken(user, tenant)

            return ResponseEntity.ok(LoginResponse(
                token = newToken,
                refreshToken = newRefreshToken,
                user = UserDto.from(user),
                tenant = TenantDto.from(tenant)
            ))

        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                LoginResponse(error = "Token refresh failed: ${e.message}")
            )
        }
    }

    @GetMapping("/me")
    suspend fun getCurrentUser(): ResponseEntity<UserDto> {
        val currentUser = CoroutineSecurityUtils.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(UserDto.from(currentUser))
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { !it.isLetterOrDigit() }
    }
}