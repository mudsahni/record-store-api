package com.muditsahni.security

import com.muditsahni.config.TenantContext
import com.muditsahni.constant.General
import com.muditsahni.error.InvalidDomainException
import com.muditsahni.error.InvalidTenantException
import com.muditsahni.error.UserAlreadyExistsException
import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.repository.global.DomainRepository
import com.muditsahni.repository.global.TenantRepository
import com.muditsahni.repository.TenantAwareUserRepository
import com.muditsahni.security.dto.request.ChangePasswordRequest
import com.muditsahni.security.dto.request.LoginRequest
import com.muditsahni.security.dto.request.RefreshTokenRequest
import com.muditsahni.security.dto.request.RegisterationRequest
import com.muditsahni.security.dto.response.LoginResponse
import com.muditsahni.security.dto.response.RegistrationResponse
import com.muditsahni.security.dto.response.TenantDto
import com.muditsahni.security.dto.response.UserDto
import com.muditsahni.service.EmailService
import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val domainRepository: DomainRepository,
    private val tenantAwareUserRepository: TenantAwareUserRepository,
    private val tenantRepository: TenantRepository,
    private val emailService: EmailService,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = KotlinLogging.logger {}

    @Value("\${jwt.verification.expiration:86400000}") // 24 hours for verification
    private val verificationExpiration: Long = 86400000

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
            val userAndTenant = findUserAndTenantByEmail(request.email)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Invalid credentials")
                )

            val (user, tenant) = userAndTenant

            if (tenant.deleted) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Tenant is inactive")
                )
            }

            // Check if account is locked
            if (user.isAccountLocked()) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Account is temporarily locked. Try again later.")
                )
            }

            TenantContext.setTenant(tenant)


            try {
                // Check if user is active
                if (user.status == UserStatus.PENDING) {
                    return ResponseEntity.badRequest().body(
                        LoginResponse(error = "Account is pending activation")
                    )
                }
                if (user.status != UserStatus.ACTIVE) {
                    return ResponseEntity.badRequest().body(
                        LoginResponse(error = "Account is unavailable")
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
                    tenantAwareUserRepository.save(updatedUser)

                    return ResponseEntity.badRequest().body(
                        LoginResponse(error = "Invalid credentials")
                    )
                }

                // Reset failed attempts and update last login
                val updatedUser = user.copy(
                    failedLoginAttempts = 0,
                    accountLockedUntil = null,
                    lastLoginAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                tenantAwareUserRepository.save(updatedUser)

                // Generate token
                val token = jwtService.generateToken(updatedUser, tenant)

                return ResponseEntity.ok(LoginResponse(
                    token = token,
                    user = UserDto.from(updatedUser),
                    tenant = TenantDto.from(tenant),
                    mustChangePassword = updatedUser.mustChangePassword
                ))


            } finally {
                TenantContext.clear()
            }


        } catch (e: Exception) {
            TenantContext.clear()
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
     * @return A [ResponseEntity] containing the registration response with a success message,
     * or an error response if registration fails.
     * @throws IllegalArgumentException if the password does not meet the required strength criteria.
     * @throws Exception if there is an error during registration, such as tenant not found or user already exists.
     */
    @Operation(
        summary = "User registration",
        description = "Handles user registration with details and tenant information, returns JWT token and user details"
    )
    @PostMapping("/register")
    suspend fun register(@RequestBody request: RegisterationRequest): ResponseEntity<RegistrationResponse> {
        return try {
            // Extract domain from email
            val emailDomain = request.email.substringAfter("@").lowercase()
            // Find tenant by domain
            val domain = domainRepository.findByNameAndDeletedFalse(emailDomain)
                ?: throw InvalidDomainException()

            // Check if tenant exists and is active
            val tenant = tenantRepository.findByName(domain.tenantName)
                ?: throw InvalidTenantException()
            if (tenant.deleted) {
                throw InvalidTenantException()
            }

            TenantContext.setTenant(tenant)

            try {
                // Check if user already exists
                logger.info("Checking for existing user with email: ${request.email}")
                val existingUser = tenantAwareUserRepository.findByEmail(request.email)
                if (existingUser != null) {
                    throw UserAlreadyExistsException("email", request.email)
                }

                val verificationToken = jwtService.generateVerificationToken(request.email, tenant.name)

                val newUser = User(
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    phoneNumber = request.phoneNumber,
                    passwordHash = passwordEncoder.encode(request.password),
                    tenantName = domain.tenantName,
                    roles = listOf(Role.USER),
                    createdBy = General.SYSTEM.name,
                    status = UserStatus.PENDING,
                    verificationToken = verificationToken,
                    verificationTokenExpiresAt = Instant.now().plusSeconds(verificationExpiration / 1000),
                    emailVerified = false
                )

                logger.info("Creating new user: ${newUser.email} in tenant: ${tenant.name}")
                val savedUser = tenantAwareUserRepository.save(newUser)

                // Send verification email
                logger.info("Sending verification email to: ${savedUser.email}")
                val emailSent = emailService.sendVerificationEmail(savedUser, verificationToken)
                if (!emailSent) {
                    // Optionally delete the user or log error
                    logger.error("Failed to send verification email for user: ${savedUser.email}")
                }
                logger.info("User registered successfully: ${savedUser.email}")
                ResponseEntity.ok(RegistrationResponse(message = "Registration successful. Please check your email to verify your account."))

            } finally {
                TenantContext.clear()
            }
        } catch (e: Exception) {
            TenantContext.clear()
            logger.error("Registration error: ${e.message}", e)
            ResponseEntity.badRequest().body(
                RegistrationResponse(error = "Registration failed: ${e.message}")
            )
        }
    }

    @Operation(
        summary = "Verify email address",
        description = "Verifies user email using the token sent via email"
    )
    @GetMapping("/verify")
    suspend fun verifyEmail(@RequestParam token: String): ResponseEntity<RegistrationResponse> {
        try {

            // Use your JwtService to parse verification token
            val tokenData = jwtService.parseVerificationToken(token)
                ?: return ResponseEntity.badRequest().body(
                    RegistrationResponse(error = "Invalid or expired verification token")
                )

            val tenant = tenantRepository.findByName(tokenData.tenantName) ?: throw InvalidTenantException()

            // Set tenant context from token
            TenantContext.setTenant(tenant)

            try {
                val user = tenantAwareUserRepository.findByVerificationToken(token)
                    ?: return ResponseEntity.badRequest().body(
                        RegistrationResponse(error = "Invalid verification token")
                    )

                // Check if token is expired
                if (user.verificationTokenExpiresAt == null ||
                    user.verificationTokenExpiresAt.isBefore(Instant.now())) {
                    return ResponseEntity.badRequest().body(
                        RegistrationResponse(error = "Verification token has expired")
                    )
                }

                // Check if already verified
                if (user.emailVerified) {
                    return ResponseEntity.badRequest().body(
                        RegistrationResponse(error = "Email is already verified")
                    )
                }

                // Update user status
                val updatedUser = user.copy(
                    status = UserStatus.ACTIVE,
                    emailVerified = true,
                    verificationToken = null,
                    verificationTokenExpiresAt = null,
                    updatedAt = Instant.now()
                )

                tenantAwareUserRepository.save(updatedUser)

                return ResponseEntity.ok(RegistrationResponse(
                    message = "Email verified successfully! You can now log in."
                ))


            } finally {
                TenantContext.clear()
            }

        } catch (e: Exception) {
            TenantContext.clear()
            return ResponseEntity.badRequest().body(
                RegistrationResponse(error = "Email verification failed: ${e.message}")
            )
        }
    }

    @PostMapping("/resend")
    suspend fun resendVerification(@RequestParam email: String): ResponseEntity<RegistrationResponse> {
        try {
            val userAndTenant = findUserAndTenantByEmail(email)
                ?: return ResponseEntity.badRequest().body(
                    RegistrationResponse(error = "User not found")
                )

            val (user, tenant) = userAndTenant
            TenantContext.setTenant(tenant)

            try {
                if (user.emailVerified) {
                    return ResponseEntity.badRequest().body(
                        RegistrationResponse(error = "Email is already verified")
                    )
                }

                // Generate new verification token using your JwtService
                val newToken = jwtService.generateVerificationToken(user.email, tenant.name)

                val updatedUser = user.copy(
                    verificationToken = newToken,
                    verificationTokenExpiresAt = Instant.now().plusSeconds(verificationExpiration / 1000),
                    updatedAt = Instant.now()
                )

                tenantAwareUserRepository.save(updatedUser)
                emailService.sendVerificationEmail(updatedUser, newToken)

                return ResponseEntity.ok(RegistrationResponse(
                    message = "Verification email sent successfully"
                ))

            } finally {
                TenantContext.clear()
            }
        } catch (e: Exception) {
            TenantContext.clear()
            return ResponseEntity.badRequest().body(
                RegistrationResponse(error = "Failed to resend verification email: ${e.message}")
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

            tenantAwareUserRepository.save(updatedUser)
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

            val user = tenantAwareUserRepository.findById(userId)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "User not found")
                )

            val tenant = tenantRepository.findByName(tenantName)
                ?: return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Tenant not found")
                )

            if (user.status != UserStatus.ACTIVE || tenant.deleted) {
                return ResponseEntity.badRequest().body(
                    LoginResponse(error = "Account or tenant is not available")
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

    private suspend fun findUserAndTenantByEmail(email: String): Pair<User, Tenant>? {
        val emailDomain = email.substringAfter("@").lowercase()
        val domain = domainRepository.findByNameAndDeletedFalse(emailDomain) ?: return null
        val tenant = tenantRepository.findByName(domain.tenantName) ?: return null
        val user = tenantAwareUserRepository.findByEmailInTenant(email, tenant.name) ?: return null
        return Pair(user, tenant)
    }
}