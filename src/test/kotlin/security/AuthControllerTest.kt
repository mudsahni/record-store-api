package security

import com.muditsahni.model.entity.User
import com.muditsahni.model.entity.Tenant
import com.muditsahni.repository.TenantRepository
import com.muditsahni.repository.UserRepository
import com.muditsahni.security.AuthController
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.security.JwtService
import com.muditsahni.security.dto.request.ChangePasswordRequest
import com.muditsahni.security.dto.request.LoginRequest
import com.muditsahni.security.dto.request.RefreshTokenRequest
import com.muditsahni.security.dto.request.RegisterRequest
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

class AuthControllerTest {

    private lateinit var userRepository: UserRepository
    private lateinit var tenantRepository: TenantRepository
    private lateinit var jwtService: JwtService
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var authController: AuthController

    private val sampleTenant = Tenant(
        id = UUID.randomUUID(),
        name = "testTenant",
        type = "BUSINESS",
        createdAt = Instant.now(),
        createdBy = "SYSTEM",
        deleted = false
    )

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        tenantRepository = mockk()
        jwtService = mockk()
        passwordEncoder = mockk()
        authController = AuthController(userRepository, tenantRepository, jwtService, passwordEncoder)

        // Mock CoroutineSecurityUtils
        mockkObject(CoroutineSecurityUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CoroutineSecurityUtils)
    }

    // Helper method to create a sample user
    private fun createSampleUser(
        email: String = "test@example.com",
        tenantName: String = "testTenant",
        isActive: Boolean = true,
        failedLoginAttempts: Int = 0,
        accountLockedUntil: Instant? = null,
        mustChangePassword: Boolean = false
    ): User {
        return User(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
            email = email,
            phoneNumber = "1234567890",
            passwordHash = "hashedPassword123",
            tenantName = tenantName,
            roles = listOf("USER"),
            isActive = isActive,
            createdAt = Instant.now(),
            createdBy = "SYSTEM",
            failedLoginAttempts = failedLoginAttempts,
            accountLockedUntil = accountLockedUntil,
            mustChangePassword = mustChangePassword
        )
    }

    // login() tests

    @Test
    fun `login returns success when credentials are valid`() = runBlocking {
        val loginRequest = LoginRequest("test@example.com", "password123")
        val user = createSampleUser()

        coEvery { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("password123", "hashedPassword123") } returns true
        coEvery { tenantRepository.findByName("testTenant") } returns sampleTenant
        coEvery { userRepository.save(any()) } returns user
        every { jwtService.generateToken(any(), any()) } returns "jwt-token"

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.token)
        assertEquals("jwt-token", response.body?.token)
        assertNotNull(response.body?.user)
        assertNotNull(response.body?.tenant)
        assertNull(response.body?.error)

        coVerify(exactly = 1) {
            userRepository.findByEmail("test@example.com")
            passwordEncoder.matches("password123", "hashedPassword123")
            tenantRepository.findByName("testTenant")
            userRepository.save(any())
            jwtService.generateToken(any(), any())
        }
    }

    @Test
    fun `login returns error when user not found`() = runBlocking {
        val loginRequest = LoginRequest("nonexistent@example.com", "password123")

        coEvery { userRepository.findByEmail("nonexistent@example.com") } returns null

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid credentials", response.body?.error)
        assertNull(response.body?.token)

        coVerify(exactly = 1) { userRepository.findByEmail("nonexistent@example.com") }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `login returns error when account is locked`() = runBlocking {
        val loginRequest = LoginRequest("test@example.com", "password123")
        val lockedUser = createSampleUser(
            accountLockedUntil = Instant.now().plusSeconds(600) // Locked for 10 more minutes
        )

        coEvery { userRepository.findByEmail("test@example.com") } returns lockedUser

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Account is temporarily locked. Try again later.", response.body?.error)
        assertNull(response.body?.token)

        coVerify(exactly = 1) { userRepository.findByEmail("test@example.com") }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `login increments failed attempts on wrong password`() = runBlocking {
        val loginRequest = LoginRequest("test@example.com", "wrongPassword")
        val user = createSampleUser(failedLoginAttempts = 2)

        coEvery { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("wrongPassword", "hashedPassword123") } returns false
        coEvery { userRepository.save(any()) } returns user

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid credentials", response.body?.error)
        assertNull(response.body?.token)

        coVerify(exactly = 1) {
            userRepository.findByEmail("test@example.com")
            passwordEncoder.matches("wrongPassword", "hashedPassword123")
            userRepository.save(match { it.failedLoginAttempts == 3 })
        }
    }

    @Test
    fun `login locks account when reaching max failed attempts`() = runBlocking {
        val loginRequest = LoginRequest("test@example.com", "wrongPassword")
        // Start with MAX_FAILED_LOGIN_ATTEMPTS - 1 to trigger lock on next attempt
        val user = createSampleUser(failedLoginAttempts = 5) // Already at max

        coEvery { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("wrongPassword", "hashedPassword123") } returns false
        coEvery { userRepository.save(any()) } returns user

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid credentials", response.body?.error)

        coVerify(exactly = 1) {
            userRepository.save(match { savedUser ->
                savedUser.failedLoginAttempts == 6 &&
                        savedUser.accountLockedUntil != null
            })
        }
    }

    @Test
    fun `login returns error when user is inactive`() = runBlocking {
        val loginRequest = LoginRequest("test@example.com", "password123")
        val inactiveUser = createSampleUser(isActive = false)

        coEvery { userRepository.findByEmail("test@example.com") } returns inactiveUser
        every { passwordEncoder.matches("password123", "hashedPassword123") } returns true

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Account is disabled", response.body?.error)
        assertNull(response.body?.token)
    }

    @Test
    fun `login returns error when tenant not found`() = runBlocking {
        val loginRequest = LoginRequest("test@example.com", "password123")
        val user = createSampleUser()

        coEvery { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("password123", "hashedPassword123") } returns true
        coEvery { tenantRepository.findByName("testTenant") } returns null

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Tenant not found", response.body?.error)
        assertNull(response.body?.token)
    }

    @Test
    fun `login returns error when tenant is deleted`() = runBlocking {
        val loginRequest = LoginRequest("test@example.com", "password123")
        val user = createSampleUser()
        val deletedTenant = sampleTenant.copy(deleted = true)

        coEvery { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("password123", "hashedPassword123") } returns true
        coEvery { tenantRepository.findByName("testTenant") } returns deletedTenant

        val response = authController.login(loginRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Tenant is inactive", response.body?.error)
        assertNull(response.body?.token)
    }

    // register() tests

    @Test
    fun `register returns success when valid request`() = runBlocking {
        val registerRequest = RegisterRequest(
            firstName = "John",
            lastName = "Doe",
            email = "new@example.com",
            phoneNumber = "1234567890",
            password = "Password123!",
            tenantName = "testTenant"
        )
        val newUser = createSampleUser(email = "new@example.com")

        coEvery { tenantRepository.findByName("testTenant") } returns sampleTenant
        coEvery { userRepository.findByEmail("new@example.com") } returns null
        every { passwordEncoder.encode("Password123!") } returns "hashedPassword123"
        coEvery { userRepository.save(any()) } returns newUser
        every { jwtService.generateToken(any(), any()) } returns "jwt-token"

        val response = authController.register(registerRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.token)
        assertEquals("jwt-token", response.body?.token)
        assertNotNull(response.body?.user)
        assertNotNull(response.body?.tenant)
        assertNull(response.body?.error)

        coVerify(exactly = 1) {
            tenantRepository.findByName("testTenant")
            userRepository.findByEmail("new@example.com")
            passwordEncoder.encode("Password123!")
            userRepository.save(any())
            jwtService.generateToken(any(), any())
        }
    }

    @Test
    fun `register returns error when password is invalid`() = runBlocking {
        val registerRequest = RegisterRequest(
            firstName = "John",
            lastName = "Doe",
            email = "new@example.com",
            phoneNumber = "1234567890",
            password = "weak", // Invalid password
            tenantName = "testTenant"
        )

        val response = authController.register(registerRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Password must be at least 8 characters with uppercase, lowercase, number, and special character", response.body?.error)
        assertNull(response.body?.token)

        // Should not proceed to check tenant or save user
        coVerify(exactly = 0) { tenantRepository.findByName(any()) }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `register returns error when tenant not found`() = runBlocking {
        val registerRequest = RegisterRequest(
            firstName = "John",
            lastName = "Doe",
            email = "new@example.com",
            phoneNumber = "1234567890",
            password = "Password123!",
            tenantName = "nonexistentTenant"
        )

        coEvery { tenantRepository.findByName("nonexistentTenant") } returns null

        val response = authController.register(registerRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Tenant not found", response.body?.error)
        assertNull(response.body?.token)
    }

    @Test
    fun `register returns error when tenant is deleted`() = runBlocking {
        val registerRequest = RegisterRequest(
            firstName = "John",
            lastName = "Doe",
            email = "new@example.com",
            phoneNumber = "1234567890",
            password = "Password123!",
            tenantName = "testTenant"
        )
        val deletedTenant = sampleTenant.copy(deleted = true)

        coEvery { tenantRepository.findByName("testTenant") } returns deletedTenant

        val response = authController.register(registerRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Tenant is inactive", response.body?.error)
        assertNull(response.body?.token)
    }

    @Test
    fun `register returns error when user already exists`() = runBlocking {
        val registerRequest = RegisterRequest(
            firstName = "John",
            lastName = "Doe",
            email = "existing@example.com",
            phoneNumber = "1234567890",
            password = "Password123!",
            tenantName = "testTenant"
        )
        val existingUser = createSampleUser(email = "existing@example.com")

        coEvery { tenantRepository.findByName("testTenant") } returns sampleTenant
        coEvery { userRepository.findByEmail("existing@example.com") } returns existingUser

        val response = authController.register(registerRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("User already exists", response.body?.error)
        assertNull(response.body?.token)

        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    // changePassword() tests

    @Test
    fun `changePassword returns success when valid request`() = runBlocking {
        val changePasswordRequest = ChangePasswordRequest("oldPassword", "NewPassword123!")
        val currentUser = createSampleUser()

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        every { passwordEncoder.matches("oldPassword", "hashedPassword123") } returns true
        every { passwordEncoder.encode("NewPassword123!") } returns "newHashedPassword"
        coEvery { userRepository.save(any()) } returns currentUser

        val response = authController.changePassword(changePasswordRequest)

        assertEquals(HttpStatus.OK, response.statusCode)

        coVerify(exactly = 1) {
            CoroutineSecurityUtils.getCurrentUser()
            passwordEncoder.matches("oldPassword", "hashedPassword123")
            passwordEncoder.encode("NewPassword123!")
            userRepository.save(match { it.passwordHash == "newHashedPassword" })
        }
    }

    @Test
    fun `changePassword returns error when no current user`() = runBlocking {
        val changePasswordRequest = ChangePasswordRequest("oldPassword", "NewPassword123!")

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns null

        val response = authController.changePassword(changePasswordRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        coVerify(exactly = 1) { CoroutineSecurityUtils.getCurrentUser() }
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `changePassword returns error when old password is wrong`() = runBlocking {
        val changePasswordRequest = ChangePasswordRequest("wrongOldPassword", "NewPassword123!")
        val currentUser = createSampleUser()

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        every { passwordEncoder.matches("wrongOldPassword", "hashedPassword123") } returns false

        val response = authController.changePassword(changePasswordRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        coVerify(exactly = 1) {
            CoroutineSecurityUtils.getCurrentUser()
            passwordEncoder.matches("wrongOldPassword", "hashedPassword123")
        }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changePassword returns error when new password is invalid`() = runBlocking {
        val changePasswordRequest = ChangePasswordRequest("oldPassword", "weak") // Invalid new password
        val currentUser = createSampleUser()

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        every { passwordEncoder.matches("oldPassword", "hashedPassword123") } returns true

        val response = authController.changePassword(changePasswordRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    // refreshToken() tests

    @Test
    fun `refreshToken returns success when valid refresh token`() = runBlocking {
        val refreshRequest = RefreshTokenRequest("valid-refresh-token")
        val user = createSampleUser()
        val userId = user.id

        every { jwtService.isTokenValid("valid-refresh-token") } returns true
        every { jwtService.isRefreshToken("valid-refresh-token") } returns true
        every { jwtService.extractUserId("valid-refresh-token") } returns userId
        every { jwtService.extractTenantName("valid-refresh-token") } returns "testTenant"
        coEvery { userRepository.findById(userId) } returns user
        coEvery { tenantRepository.findByName("testTenant") } returns sampleTenant
        every { jwtService.generateToken(any(), any()) } returns "new-jwt-token"
        every { jwtService.generateRefreshToken(any(), any()) } returns "new-refresh-token"

        val response = authController.refreshToken(refreshRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("new-jwt-token", response.body?.token)
        assertEquals("new-refresh-token", response.body?.refreshToken)
        assertNotNull(response.body?.user)
        assertNotNull(response.body?.tenant)
        assertNull(response.body?.error)
    }

    @Test
    fun `refreshToken returns error when token is invalid`() = runBlocking {
        val refreshRequest = RefreshTokenRequest("invalid-refresh-token")

        every { jwtService.isTokenValid("invalid-refresh-token") } returns false

        val response = authController.refreshToken(refreshRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid refresh token", response.body?.error)
        assertNull(response.body?.token)
    }

    @Test
    fun `refreshToken returns error when token is not refresh token`() = runBlocking {
        val refreshRequest = RefreshTokenRequest("access-token")

        every { jwtService.isTokenValid("access-token") } returns true
        every { jwtService.isRefreshToken("access-token") } returns false

        val response = authController.refreshToken(refreshRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Invalid refresh token", response.body?.error)
        assertNull(response.body?.token)
    }

    // getCurrentUser() tests

    @Test
    fun `getCurrentUser returns user when authenticated`() = runBlocking {
        val currentUser = createSampleUser()

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser

        val response = authController.getCurrentUser()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(currentUser.email, response.body?.email)

        coVerify(exactly = 1) { CoroutineSecurityUtils.getCurrentUser() }
    }

    @Test
    fun `getCurrentUser returns unauthorized when not authenticated`() = runBlocking {
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns null

        val response = authController.getCurrentUser()

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNull(response.body)

        coVerify(exactly = 1) { CoroutineSecurityUtils.getCurrentUser() }
    }
}