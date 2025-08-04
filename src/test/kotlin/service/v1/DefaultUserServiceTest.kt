package service.v1

import com.muditsahni.constant.General
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.repository.global.TenantRepository
import com.muditsahni.repository.TenantAwareUserRepository
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

class DefaultUserServiceTest {

    private lateinit var tenantAwareUserRepository: TenantAwareUserRepository
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
        createdBy = General.SYSTEM.toString(),
        domains = setOf("example.com"),
    )

    @BeforeEach
    fun setup() {
        tenantAwareUserRepository = mockk()
        tenantRepository = mockk()
        passwordEncoder = mockk()
        userService = DefaultUserService(tenantAwareUserRepository, tenantRepository, passwordEncoder)

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
        status: UserStatus = UserStatus.ACTIVE,
    ): User {
        return User(
            id = UUID.randomUUID(),
            tenantName = tenantName,
            firstName = "name-first",
            lastName = "name-last",
            email = email,
            phoneNumber = phoneNumber,
            passwordHash = "hashedPassword",
            status = status,
            createdAt = Instant.now(),
            updatedAt = null,
            createdBy = General.SYSTEM.toString(),
            updatedBy = null,
        )
    }

    // getUserByPhoneNumber() tests

    @Test
    fun `getUserByPhoneNumber returns user when active and tenant matches`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(phoneNumber = "555")
        coEvery { tenantAwareUserRepository.findByPhoneNumber("555") } returns user

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
        coEvery { tenantAwareUserRepository.findByPhoneNumber("555") } returns user

        val result = userService.getUserByPhoneNumber("555", "t1")
        assertNull(result)
    }

    // getUserByEmail() tests

    @Test
    fun `getUserByEmail returns user when active and tenant matches`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns user

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
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns user

        val result = userService.getUserByEmail("u@t.com", "t1")
        assertNull(result)
    }

    // deactivateUserByEmail() tests

    @Test
    fun `deactivateUserByEmail returns false when user not found`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { tenantAwareUserRepository.findByEmail("x@x.com") } returns null

        val result = userService.deactivateUserByEmail("x@x.com", "t1")
        assertFalse(result)
    }

    @Test
    fun `deactivateUserByEmail returns false when already inactive`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(status = UserStatus.INACTIVE)
        coEvery { tenantAwareUserRepository.findByEmail("x@x.com") } returns user

        val result = userService.deactivateUserByEmail("x@x.com", "t1")
        assertFalse(result)
        coVerify(exactly = 0) { tenantAwareUserRepository.save(any()) }
    }

    @Test
    fun `deactivateUserByEmail deactivates and saves when active`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns user
        coEvery { tenantAwareUserRepository.save(any()) } returnsArgument 0

        val result = userService.deactivateUserByEmail("u@t.com", "t1")
        assertTrue(result)
        assertTrue(user.status == UserStatus.INACTIVE)
        assertNotNull(user.updatedAt)
        assertEquals("test@example.com", user.updatedBy) // Should use current user
        coVerify(exactly = 1) { tenantAwareUserRepository.save(user) }
    }

    // deactivateUserByPhoneNumber() tests

    @Test
    fun `deactivateUserByPhoneNumber returns false when user not found`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { tenantAwareUserRepository.findByPhoneNumber("000") } returns null

        val result = userService.deactivateUserByPhoneNumber("000", "t1")
        assertFalse(result)
    }

    @Test
    fun `deactivateUserByPhoneNumber returns false when already inactive`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser(status = UserStatus.PENDING)
        coEvery { tenantAwareUserRepository.findByPhoneNumber("000") } returns user

        val result = userService.deactivateUserByPhoneNumber("000", "t1")
        assertFalse(result)
        coVerify(exactly = 0) { tenantAwareUserRepository.save(any()) }
    }

    @Test
    fun `deactivateUserByPhoneNumber deactivates and saves when active`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { tenantAwareUserRepository.findByPhoneNumber("555") } returns user
        coEvery { tenantAwareUserRepository.save(any()) } returnsArgument 0

        val result = userService.deactivateUserByPhoneNumber("555", "t1")
        assertTrue(result)
        assertTrue(user.status == UserStatus.INACTIVE)
        assertNotNull(user.updatedAt)
        assertEquals("test@example.com", user.updatedBy) // Should use current user
        coVerify(exactly = 1) { tenantAwareUserRepository.save(user) }
    }

    // changePassword() tests

    @Test
    fun `changePassword returns false when user not found`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns null

        val result = userService.changePassword("u@t.com", "t1", "oldPass", "newPass")
        assertFalse(result)
    }

    @Test
    fun `changePassword returns false when old password is incorrect`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns user
        every { passwordEncoder.matches("wrongPass", user.passwordHash) } returns false

        val result = userService.changePassword("u@t.com", "t1", "wrongPass", "newPass")
        assertFalse(result)
        coVerify(exactly = 0) { tenantAwareUserRepository.save(any()) }
    }

    @Test
    fun `changePassword succeeds when old password is correct`() = runBlocking {
        coEvery { tenantRepository.findByName("t1") } returns sampleTenant
        val user = createSampleUser()
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns user
        every { passwordEncoder.matches("oldPass", user.passwordHash) } returns true
        every { passwordEncoder.encode("newPass") } returns "newHashedPass"
        coEvery { tenantAwareUserRepository.save(any()) } returnsArgument 0

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
        coVerify(exactly = 1) { tenantAwareUserRepository.save(user) }
    }

    // findUserForAuthentication() tests

    @Test
    fun `findUserForAuthentication returns user when found`() = runBlocking {
        val user = createSampleUser()
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns user

        val result = userService.findUserForAuthentication("u@t.com")
        assertEquals(user, result)
        coVerify(exactly = 1) { tenantAwareUserRepository.findByEmail("u@t.com") }
    }

    @Test
    fun `findUserForAuthentication returns null when not found`() = runBlocking {
        coEvery { tenantAwareUserRepository.findByEmail("u@t.com") } returns null

        val result = userService.findUserForAuthentication("u@t.com")
        assertNull(result)
        coVerify(exactly = 1) { tenantAwareUserRepository.findByEmail("u@t.com") }
    }
}