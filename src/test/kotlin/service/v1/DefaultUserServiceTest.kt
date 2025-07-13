package service.v1

import com.muditsahni.constant.General
import com.muditsahni.error.UserAlreadyExistsException
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.repository.TenantRepository
import com.muditsahni.repository.UserRepository
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.v1.DefaultUserService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.assertThrows

class DefaultUserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var tenantRepository: TenantRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var userService: DefaultUserService

    private val sampleTenant = Tenant(
        id = UUID.randomUUID(),
        name = "t1",
        type = "type1",
        createdAt = Instant.now(),
        updatedAt = null,
        deleted = false,
        createdBy = General.SYSTEM.toString()
    )

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        tenantRepository = mockk()
        passwordEncoder = mockk()
        userService = DefaultUserService(userRepository, tenantRepository, passwordEncoder)

        // Mock CoroutineSecurityUtils
        mockkObject(CoroutineSecurityUtils)
        coEvery { CoroutineSecurityUtils.getCurrentUserEmail() } returns "test@example.com"
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CoroutineSecurityUtils)
    }

    // Helper method to create a user for testing
    private fun createSampleUser(
        email: String = "u@t.com",
        phoneNumber: String = "555",
        tenantName: String = "t1",
        isActive: Boolean = true
    ): User {
        return User(
            id = UUID.randomUUID(),
            tenantName = tenantName,
            email = email,
            phoneNumber = phoneNumber,
            passwordHash = "hashedPassword",
            isActive = isActive,
            createdAt = Instant.now(),
            updatedAt = null,
            createdBy = General.SYSTEM.toString(),
            updatedBy = null
        )
    }

    // createAuthUser() tests - Updated to use the public method

    @Test
    fun `createAuthUser throws if tenant does not exist`() = runBlocking {
        coEvery { tenantRepository.findByName("nope") } returns null

        val ex = assertThrows<IllegalArgumentException> {
            runBlocking {
                userService.createAuthUser("a@b.com", "nope", "123", "password")
            }
        }
        assertEquals("Tenant with name nope does not exist.", ex.message)
        coVerify(exactly = 1) { tenantRepository.findByName("nope") }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `createAuthUser throws if tenant is deleted`() = runBlocking {
        val deletedTenant = sampleTenant.copy(deleted = true)
        coEvery { tenantRepository.findByName("t1") } returns deletedTenant

        val ex = assertThrows<IllegalArgumentException> {
            runBlocking {
                userService.createAuthUser("a@b.com", "t1", "123", "password")
            }
        }
        assertEquals("Tenant t1 is inactive.", ex.message)
        coVerify(exactly = 1) { tenantRepository.findByName("t1") }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `createAuthUser throws if email already exists`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByEmail("u@t.com") } returns createSampleUser()

        val ex = assertThrows<UserAlreadyExistsException> {
            runBlocking {
                userService.createAuthUser("u@t.com", "t1", "777", "password")
            }
        }
        assertTrue(ex.message!!.contains("email"))
        coVerify(exactly = 1) { userRepository.findByEmail("u@t.com") }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `createAuthUser throws if phone already exists`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByEmail(any()) } returns null
        coEvery { userRepository.findByPhoneNumber("555") } returns createSampleUser(phoneNumber = "555")

        val ex = assertThrows<UserAlreadyExistsException> {
            runBlocking {
                userService.createAuthUser("new@t.com", "t1", "555", "password")
            }
        }
        assertTrue(ex.message!!.contains("phoneNumber"))
        coVerify(exactly = 1) { userRepository.findByPhoneNumber("555") }
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `createAuthUser throws when password is empty`() = runBlocking {
        val ex = assertThrows<IllegalArgumentException> {
            runBlocking {
                userService.createAuthUser("u@t.com", "t1", "555", "")
            }
        }
        assertEquals("Password cannot be empty for auth users", ex.message)
    }

    @Test
    fun `createAuthUser throws when password is blank`() = runBlocking {
        val ex = assertThrows<IllegalArgumentException> {
            runBlocking {
                userService.createAuthUser("u@t.com", "t1", "555", "   ")
            }
        }
        assertEquals("Password cannot be empty for auth users", ex.message)
    }

    @Test
    fun `createAuthUser succeeds when no conflicts`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByEmail("u@t.com") } returns null
        coEvery { userRepository.findByPhoneNumber("555") } returns null
        every { passwordEncoder.encode("password123") } returns "hashedPassword123"

        val captured = mutableListOf<User>()
        coEvery { userRepository.save(capture(captured)) } answers { firstArg() }

        val result = userService.createAuthUser("u@t.com", "t1", "555", "password123")

        assertEquals("u@t.com", result.email)
        assertEquals("555", result.phoneNumber)
        assertEquals("t1", result.tenantName)
        assertEquals("hashedPassword123", result.passwordHash)
        assertEquals(listOf("USER"), result.roles) // Default role
        assertTrue(result.isActive)
        assertNotNull(result.createdAt)
        assertEquals("test@example.com", result.createdBy) // Should use current user

        verify(exactly = 1) { passwordEncoder.encode("password123") }
        coVerify(exactly = 1) {
            tenantRepository.findByName("t1")
            userRepository.findByEmail("u@t.com")
            userRepository.findByPhoneNumber("555")
            userRepository.save(any())
        }

        // Verify the object passed into save
        val saved = captured.single()
        assertEquals("u@t.com", saved.email)
        assertEquals("555", saved.phoneNumber)
        assertEquals("t1", saved.tenantName)
        assertEquals("hashedPassword123", saved.passwordHash)
    }

    @Test
    fun `createAuthUser successfully with first and last name and custom roles`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByEmail("u@t.com") } returns null
        coEvery { userRepository.findByPhoneNumber("555") } returns null
        every { passwordEncoder.encode("password123") } returns "hashedPassword123"

        val captured = mutableListOf<User>()
        coEvery { userRepository.save(capture(captured)) } answers { firstArg() }

        val result = userService.createAuthUser(
            email = "u@t.com",
            tenantName = "t1",
            phoneNumber = "555",
            password = "password123",
            firstName = "John",
            lastName = "Doe",
            roles = listOf("USER", "ADMIN")
        )

        assertEquals("u@t.com", result.email)
        assertEquals("555", result.phoneNumber)
        assertEquals("t1", result.tenantName)
        assertEquals("John", result.firstName)
        assertEquals("Doe", result.lastName)
        assertEquals("hashedPassword123", result.passwordHash)
        assertEquals(listOf("USER", "ADMIN"), result.roles)
        assertTrue(result.isActive)
        assertNotNull(result.createdAt)

        verify(exactly = 1) { passwordEncoder.encode("password123") }
        coVerify(exactly = 1) {
            tenantRepository.findByName("t1")
            userRepository.findByEmail("u@t.com")
            userRepository.findByPhoneNumber("555")
            userRepository.save(any())
        }

        // Verify the object passed into save
        val saved = captured.single()
        assertEquals("John", saved.firstName)
        assertEquals("Doe", saved.lastName)
        assertEquals(listOf("USER", "ADMIN"), saved.roles)
    }

    @Test
    fun `createAuthUser falls back to SYSTEM when no current user`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByEmail("u@t.com") } returns null
        coEvery { userRepository.findByPhoneNumber("555") } returns null
        every { passwordEncoder.encode("password123") } returns "hashedPassword123"
        coEvery { CoroutineSecurityUtils.getCurrentUserEmail() } throws Exception("No user")

        val captured = mutableListOf<User>()
        coEvery { userRepository.save(capture(captured)) } answers { firstArg() }

        val result = userService.createAuthUser("u@t.com", "t1", "555", "password123")

        assertEquals(General.SYSTEM.toString(), result.createdBy)
        val saved = captured.single()
        assertEquals(General.SYSTEM.toString(), saved.createdBy)
    }

    // getUserByPhoneNumber() tests

    @Test
    fun `getUserByPhoneNumber returns user when active and tenant matches`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(phoneNumber = "555")
        coEvery { userRepository.findByPhoneNumber("555") } returns user

        val result = userService.getUserByPhoneNumber("555", "t1")
        assertEquals(user, result)
    }

    @Test
    fun `getUserByPhoneNumber returns null when tenant is deleted`() = runBlocking {
        val deletedTenant = sampleTenant.copy(deleted = true)
        coEvery { tenantRepository.findByName("t1") } returns deletedTenant

        val result = userService.getUserByPhoneNumber("555", "t1")
        assertNull(result)
    }

    @Test
    fun `getUserByPhoneNumber returns null if tenant mismatch`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(tenantName = "other")
        coEvery { userRepository.findByPhoneNumber("555") } returns user

        val result = userService.getUserByPhoneNumber("555", "t1")
        assertNull(result)
    }

    @Test
    fun `getUserByPhoneNumber returns null if inactive`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(isActive = false)
        coEvery { userRepository.findByPhoneNumber("555") } returns user

        val result = userService.getUserByPhoneNumber("555", "t1")
        assertNull(result)
    }

    // getUserByEmail() tests

    @Test
    fun `getUserByEmail returns user when active and tenant matches`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { userRepository.findByEmail("u@t.com") } returns user

        val result = userService.getUserByEmail("u@t.com", "t1")
        assertEquals(user, result)
    }

    @Test
    fun `getUserByEmail returns null when tenant is deleted`() = runBlocking {
        val deletedTenant = sampleTenant.copy(deleted = true)
        coEvery { tenantRepository.findByName("t1") } returns deletedTenant

        val result = userService.getUserByEmail("u@t.com", "t1")
        assertNull(result)
    }

    @Test
    fun `getUserByEmail returns null if tenant mismatch`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(tenantName = "other")
        coEvery { userRepository.findByEmail("u@t.com") } returns user

        val result = userService.getUserByEmail("u@t.com", "t1")
        assertNull(result)
    }

    @Test
    fun `getUserByEmail returns null if inactive`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(isActive = false)
        coEvery { userRepository.findByEmail("u@t.com") } returns user

        val result = userService.getUserByEmail("u@t.com", "t1")
        assertNull(result)
    }

    // deactivateUserByEmail() tests

    @Test
    fun `deactivateUserByEmail returns false when user not found`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByEmail("x@x.com") } returns null

        val result = userService.deactivateUserByEmail("x@x.com", "t1")
        assertFalse(result)
    }

    @Test
    fun `deactivateUserByEmail returns false when already inactive`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(isActive = false)
        coEvery { userRepository.findByEmail("x@x.com") } returns user

        val result = userService.deactivateUserByEmail("x@x.com", "t1")
        assertFalse(result)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `deactivateUserByEmail deactivates and saves when active`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { userRepository.findByEmail("u@t.com") } returns user
        coEvery { userRepository.save(any()) } returnsArgument 0

        val result = userService.deactivateUserByEmail("u@t.com", "t1")
        assertTrue(result)
        assertFalse(user.isActive)
        assertNotNull(user.updatedAt)
        assertEquals("test@example.com", user.updatedBy) // Should use current user
        coVerify(exactly = 1) { userRepository.save(user) }
    }

    // deactivateUserByPhoneNumber() tests

    @Test
    fun `deactivateUserByPhoneNumber returns false when user not found`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByPhoneNumber("000") } returns null

        val result = userService.deactivateUserByPhoneNumber("000", "t1")
        assertFalse(result)
    }

    @Test
    fun `deactivateUserByPhoneNumber returns false when already inactive`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(isActive = false)
        coEvery { userRepository.findByPhoneNumber("000") } returns user

        val result = userService.deactivateUserByPhoneNumber("000", "t1")
        assertFalse(result)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `deactivateUserByPhoneNumber deactivates and saves when active`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { userRepository.findByPhoneNumber("555") } returns user
        coEvery { userRepository.save(any()) } returnsArgument 0

        val result = userService.deactivateUserByPhoneNumber("555", "t1")
        assertTrue(result)
        assertFalse(user.isActive)
        assertNotNull(user.updatedAt)
        assertEquals("test@example.com", user.updatedBy) // Should use current user
        coVerify(exactly = 1) { userRepository.save(user) }
    }

    // changePassword() tests

    @Test
    fun `changePassword returns false when user not found`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { userRepository.findByEmail("u@t.com") } returns null

        val result = userService.changePassword("u@t.com", "t1", "oldPass", "newPass")
        assertFalse(result)
    }

    @Test
    fun `changePassword returns false when old password is incorrect`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { userRepository.findByEmail("u@t.com") } returns user
        every { passwordEncoder.matches("wrongPass", user.passwordHash) } returns false

        val result = userService.changePassword("u@t.com", "t1", "wrongPass", "newPass")
        assertFalse(result)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `changePassword succeeds when old password is correct`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { userRepository.findByEmail("u@t.com") } returns user
        every { passwordEncoder.matches("oldPass", user.passwordHash) } returns true
        every { passwordEncoder.encode("newPass") } returns "newHashedPass"
        coEvery { userRepository.save(any()) } returnsArgument 0

        val result = userService.changePassword("u@t.com", "t1", "oldPass", "newPass")
        assertTrue(result)
        assertEquals("newHashedPass", user.passwordHash)
        assertNotNull(user.passwordChangedAt)
        assertFalse(user.mustChangePassword)
        assertNotNull(user.updatedAt)
        assertEquals("test@example.com", user.updatedBy)

        verify(exactly = 1) {
            passwordEncoder.matches("oldPass", "hashedPassword")
            passwordEncoder.encode("newPass")
        }
        coVerify(exactly = 1) { userRepository.save(user) }
    }

    // findUserForAuthentication() tests

    @Test
    fun `findUserForAuthentication returns user when found`() = runBlocking {
        val user = createSampleUser()
        coEvery { userRepository.findByEmail("u@t.com") } returns user

        val result = userService.findUserForAuthentication("u@t.com")
        assertEquals(user, result)
        coVerify(exactly = 1) { userRepository.findByEmail("u@t.com") }
    }

    @Test
    fun `findUserForAuthentication returns null when not found`() = runBlocking {
        coEvery { userRepository.findByEmail("u@t.com") } returns null

        val result = userService.findUserForAuthentication("u@t.com")
        assertNull(result)
        coVerify(exactly = 1) { userRepository.findByEmail("u@t.com") }
    }
}