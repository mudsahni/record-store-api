package security

import com.muditsahni.config.TenantContext
import com.muditsahni.model.entity.Domain
import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.repository.TenantAwareUserRepository
import com.muditsahni.repository.global.DomainRepository
import com.muditsahni.repository.global.TenantRepository
import com.muditsahni.security.AuthController
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.security.JwtService
import com.muditsahni.security.VerificationTokenData
import com.muditsahni.security.dto.request.ChangePasswordRequest
import com.muditsahni.security.dto.request.LoginRequest
import com.muditsahni.security.dto.request.RefreshTokenRequest
import com.muditsahni.security.dto.request.RegisterationRequest
import com.muditsahni.service.EmailService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.*
import org.junit.jupiter.api.Assertions.*

class AuthControllerTest {

    private lateinit var authController: AuthController
    private lateinit var domainRepository: DomainRepository
    private lateinit var tenantAwareUserRepository: TenantAwareUserRepository
    private lateinit var tenantRepository: TenantRepository
    private lateinit var emailService: EmailService
    private lateinit var jwtService: JwtService
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testUser: User
    private lateinit var testTenant: Tenant
    private lateinit var testDomain: Domain

    @BeforeEach
    fun setUp() {
        domainRepository = mockk()
        tenantAwareUserRepository = mockk()
        tenantRepository = mockk()
        emailService = mockk()
        jwtService = mockk()
        passwordEncoder = mockk()

        authController = AuthController(
            domainRepository = domainRepository,
            tenantAwareUserRepository = tenantAwareUserRepository,
            tenantRepository = tenantRepository,
            emailService = emailService,
            jwtService = jwtService,
            passwordEncoder = passwordEncoder
        )

        // Set verification expiration
        ReflectionTestUtils.setField(authController, "verificationExpiration", 86400000L)

        // Create test data
        testTenant = Tenant(
            id = UUID.randomUUID(),
            name = "test-tenant",
            type = "organization",
            createdBy = "admin",
            createdAt = Instant.now(),
            deleted = false,
            domains = setOf("example.com")
        )

        testUser = User(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            phoneNumber = "+1234567890",
            passwordHash = "hashedPassword",
            tenantName = testTenant.name,
            roles = listOf(Role.USER),
            createdBy = "system",
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            emailVerified = true,
            failedLoginAttempts = 0,
            accountLockedUntil = null,
            lastLoginAt = Instant.now()
        )

        testDomain = Domain(
            id = UUID.randomUUID(),
            name = "example.com",
            tenantName = testTenant.name,
            createdAt = Instant.now(),
            createdBy = "admin",
            deleted = false
        )

        // Mock TenantContext as static
        mockkObject(TenantContext)
        every { TenantContext.setTenant(any<Tenant>()) } just Runs
        every { TenantContext.clear() } just Runs
    }

    @Test
    fun `login should return success with valid credentials`() = runTest {
        // Given
        val loginRequest = LoginRequest("john.doe@example.com", "password123")
        val expectedToken = "jwt-token"

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByEmailInTenant(testUser.email, testTenant.name) } returns testUser
        every { passwordEncoder.matches("password123", testUser.passwordHash) } returns true
        every { jwtService.generateToken(any(), any()) } returns expectedToken
        coEvery { tenantAwareUserRepository.save(any()) } returns testUser

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(expectedToken, response.body!!.token)
        assertNotNull(response.body!!.user)
        assertNotNull(response.body!!.tenant)

        verify { TenantContext.setTenant(testTenant) }
        verify { TenantContext.clear() }
        coVerify { tenantAwareUserRepository.save(any()) } // Should update last login
    }

    @Test
    fun `login should return error with invalid email`() = runTest {
        // Given
        val loginRequest = LoginRequest("nonexistent@example.com", "password123")

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns null

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Invalid credentials", response.body!!.error)
    }

    @Test
    fun `login should return error with invalid password`() = runTest {
        // Given
        val loginRequest = LoginRequest("john.doe@example.com", "wrongpassword")

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByEmailInTenant(testUser.email, testTenant.name) } returns testUser
        every { passwordEncoder.matches("wrongpassword", testUser.passwordHash) } returns false
        coEvery { tenantAwareUserRepository.save(any()) } returns testUser.copy(failedLoginAttempts = 1)

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Invalid credentials", response.body!!.error)

        coVerify { tenantAwareUserRepository.save(match { it.failedLoginAttempts == 1 }) }
    }

    @Test
    fun `login should return error for pending user`() = runTest {
        // Given
        val pendingUser = testUser.copy(status = UserStatus.PENDING)
        val loginRequest = LoginRequest("john.doe@example.com", "password123")

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByEmailInTenant(testUser.email, testTenant.name) } returns pendingUser

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Account is pending activation", response.body!!.error)
    }

    @Test
    fun `login should return error for deleted tenant`() = runTest {
        // Given
        val deletedTenant = testTenant.copy(deleted = true)
        val loginRequest = LoginRequest("john.doe@example.com", "password123")

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testTenant.name) } returns deletedTenant
        coEvery { tenantAwareUserRepository.findByEmailInTenant(testUser.email, testTenant.name) } returns testUser

        // When
        val response = authController.login(loginRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Tenant is inactive", response.body!!.error)
    }

    @Test
    fun `register should create user successfully`() = runTest {
        // Given
        val registrationRequest = RegisterationRequest(
            firstName = "Jane",
            lastName = "Smith",
            email = "jane.smith@example.com",
            phoneNumber = "+1234567890",
            password = "Password123!"
        )
        val verificationToken = "verification-token"

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testDomain.tenantName) } returns testTenant
        coEvery { tenantAwareUserRepository.findByEmail(registrationRequest.email) } returns null
        every { jwtService.generateVerificationToken(registrationRequest.email, testTenant.name) } returns verificationToken
        every { passwordEncoder.encode(registrationRequest.password) } returns "hashedPassword"
        coEvery { tenantAwareUserRepository.save(any()) } returns testUser
        coEvery { emailService.sendVerificationEmail(any(), any()) } returns true

        // When
        val response = authController.register(registrationRequest)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Registration successful. Please check your email to verify your account.", response.body!!.message)

        coVerify { tenantAwareUserRepository.save(match { it.status == UserStatus.PENDING }) }
        coVerify { emailService.sendVerificationEmail(any(), verificationToken) }
        verify { TenantContext.setTenant(testTenant) }
        verify { TenantContext.clear() }
    }

    @Test
    fun `register should return error for invalid domain`() = runTest {
        // Given
        val registrationRequest = RegisterationRequest(
            firstName = "Jane",
            lastName = "Smith",
            email = "jane.smith@invalid.com",
            phoneNumber = "+1234567890",
            password = "Password123!"
        )

        coEvery { domainRepository.findByNameAndDeletedFalse("invalid.com") } returns null

        // When
        val response = authController.register(registrationRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.error!!.contains("Registration failed"))
    }

    @Test
    fun `register should return error for existing user`() = runTest {
        // Given
        val registrationRequest = RegisterationRequest(
            firstName = "Jane",
            lastName = "Smith",
            email = "jane.smith@example.com",
            phoneNumber = "+1234567890",
            password = "Password123!"
        )

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testDomain.tenantName) } returns testTenant
        coEvery { tenantAwareUserRepository.findByEmail(registrationRequest.email) } returns testUser

        // When
        val response = authController.register(registrationRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.error!!.contains("Registration failed"))
    }

    @Test
    fun `verifyEmail should verify user successfully`() = runTest {
        // Given
        val token = "verification-token"
        val tokenData = VerificationTokenData("john.doe@example.com", testTenant.name)
        val pendingUser = testUser.copy(
            status = UserStatus.PENDING,
            emailVerified = false,
            verificationToken = token,
            verificationTokenExpiresAt = Instant.now().plusSeconds(3600)
        )

        every { jwtService.parseVerificationToken(token) } returns tokenData
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByVerificationToken(token) } returns pendingUser
        coEvery { tenantAwareUserRepository.save(any()) } returns pendingUser.copy(
            status = UserStatus.ACTIVE,
            emailVerified = true
        )

        // When
        val response = authController.verifyEmail(token)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Email verified successfully! You can now log in.", response.body!!.message)

        coVerify {
            tenantAwareUserRepository.save(match {
                it.status == UserStatus.ACTIVE && it.emailVerified && it.verificationToken == null
            })
        }
        verify { TenantContext.setTenant(testTenant) }
        verify { TenantContext.clear() }
    }

    @Test
    fun `verifyEmail should return error for invalid token`() = runTest {
        // Given
        val token = "invalid-token"

        every { jwtService.parseVerificationToken(token) } returns null

        // When
        val response = authController.verifyEmail(token)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Invalid or expired verification token", response.body!!.error)
    }

    @Test
    fun `verifyEmail should return error for expired token`() = runTest {
        // Given
        val token = "verification-token"
        val tokenData = VerificationTokenData("john.doe@example.com", testTenant.name)
        val pendingUser = testUser.copy(
            status = UserStatus.PENDING,
            emailVerified = false,
            verificationToken = token,
            verificationTokenExpiresAt = Instant.now().minusSeconds(3600) // Expired
        )

        every { jwtService.parseVerificationToken(token) } returns tokenData
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByVerificationToken(token) } returns pendingUser

        // When
        val response = authController.verifyEmail(token)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Verification token has expired", response.body!!.error)
    }

    @Test
    fun `verifyEmail should return error for already verified user`() = runTest {
        // Given
        val token = "verification-token"
        val tokenData = VerificationTokenData("john.doe@example.com", testTenant.name)
        val verifiedUser = testUser.copy(
            emailVerified = true,
            verificationToken = token,
            verificationTokenExpiresAt = Instant.now().plusSeconds(3600)
        )

        every { jwtService.parseVerificationToken(token) } returns tokenData
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByVerificationToken(token) } returns verifiedUser

        // When
        val response = authController.verifyEmail(token)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Email is already verified", response.body?.error)
    }

    @Test
    fun `resendVerification should send new token successfully`() = runTest {
        // Given
        val email = "john.doe@example.com"
        val newToken = "new-verification-token"
        val pendingUser = testUser.copy(emailVerified = false)

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByEmailInTenant(email, testTenant.name) } returns pendingUser
        every { jwtService.generateVerificationToken(email, testTenant.id.toString()) } returns newToken
        coEvery { tenantAwareUserRepository.save(any()) } returns pendingUser
        coEvery { emailService.sendVerificationEmail(any(), any()) } returns true

        // When
        val response = authController.resendVerification(email)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Verification email sent successfully", response.body!!.message)

        coVerify { tenantAwareUserRepository.save(match { it.verificationToken == newToken }) }
        coVerify { emailService.sendVerificationEmail(any(), newToken) }
    }

    @Test
    fun `resendVerification should return error for already verified user`() = runTest {
        // Given
        val email = "john.doe@example.com"

        coEvery { domainRepository.findByNameAndDeletedFalse("example.com") } returns testDomain
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        coEvery { tenantAwareUserRepository.findByEmailInTenant(email, testTenant.name) } returns testUser // Already verified

        // When
        val response = authController.resendVerification(email)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Email is already verified", response.body!!.error)
    }

    @Test
    fun `refreshToken should generate new tokens successfully`() = runTest {
        // Given
        val refreshTokenRequest = RefreshTokenRequest("valid-refresh-token")
        val newAccessToken = "new-access-token"
        val newRefreshToken = "new-refresh-token"

        every { jwtService.isTokenValid("valid-refresh-token") } returns true
        every { jwtService.isRefreshToken("valid-refresh-token") } returns true
        every { jwtService.extractUserId("valid-refresh-token") } returns testUser.id
        every { jwtService.extractTenantName("valid-refresh-token") } returns testTenant.name
        coEvery { tenantAwareUserRepository.findById(testUser.id) } returns testUser
        coEvery { tenantRepository.findByName(testTenant.name) } returns testTenant
        every { jwtService.generateToken(testUser, testTenant) } returns newAccessToken
        every { jwtService.generateRefreshToken(testUser, testTenant) } returns newRefreshToken

        // When
        val response = authController.refreshToken(refreshTokenRequest)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(newAccessToken, response.body!!.token)
        assertEquals(newRefreshToken, response.body!!.refreshToken)
    }

    @Test
    fun `refreshToken should return error for invalid token`() = runTest {
        // Given
        val refreshTokenRequest = RefreshTokenRequest("invalid-refresh-token")

        every { jwtService.isTokenValid("invalid-refresh-token") } returns false

        // When
        val response = authController.refreshToken(refreshTokenRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Invalid refresh token", response.body!!.error)
    }

    @Test
    fun `changePassword should update password successfully`() = runTest {
        // Given
        val changePasswordRequest = ChangePasswordRequest("oldPassword", "NewPassword123!")

        mockkObject(CoroutineSecurityUtils)
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        every { passwordEncoder.matches("oldPassword", testUser.passwordHash) } returns true
        every { passwordEncoder.encode("NewPassword123!") } returns "newHashedPassword"
        coEvery { tenantAwareUserRepository.save(any()) } returns testUser

        // When
        val response = authController.changePassword(changePasswordRequest)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)

        coVerify {
            tenantAwareUserRepository.save(match {
                it.passwordHash == "newHashedPassword" && !it.mustChangePassword
            })
        }
    }

    @Test
    fun `changePassword should return error for wrong old password`() = runTest {
        // Given
        val changePasswordRequest = ChangePasswordRequest("wrongOldPassword", "NewPassword123!")

        mockkObject(CoroutineSecurityUtils)
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        every { passwordEncoder.matches("wrongOldPassword", testUser.passwordHash) } returns false

        // When
        val response = authController.changePassword(changePasswordRequest)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `getCurrentUser should return current user`() = runTest {
        // Given
        mockkObject(CoroutineSecurityUtils)
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser

        // When
        val response = authController.getCurrentUser()

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(testUser.email, response.body!!.email)
    }

    @Test
    fun `getCurrentUser should return unauthorized when no user`() = runTest {
        // Given
        mockkObject(CoroutineSecurityUtils)
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns null

        // When
        val response = authController.getCurrentUser()

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}