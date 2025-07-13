package controller.v1

import com.muditsahni.controller.v1.UserController
import com.muditsahni.model.entity.User
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.dto.request.CreateUserRequestDto
import com.muditsahni.model.dto.response.toUserResponseDto
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.security.dto.request.ChangePasswordRequest
import com.muditsahni.service.v1.DefaultUserService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.assertThrows

class UserControllerTest {

    private lateinit var userService: DefaultUserService
    private lateinit var userController: UserController

    @BeforeEach
    fun setUp() {
        userService = mockk()
        userController = UserController(userService)
        mockkObject(CoroutineSecurityUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CoroutineSecurityUtils)
    }

    // Helper method to create a mock tenant
    private fun createMockTenant(name: String = "tenantA"): Tenant {
        return Tenant(
            id = UUID.randomUUID(),
            name = name,
            type = "BUSINESS",
            createdAt = Instant.now(),
            createdBy = "system"
        )
    }

    // Helper method to create a mock user
    private fun createMockUser(
        tenantName: String = "tenantA",
        email: String = "u@t.com",
        phoneNumber: String = "5551234"
    ): User {
        return User(
            id = UUID.randomUUID(),
            tenantName = tenantName,
            email = email,
            phoneNumber = phoneNumber,
            passwordHash = "hashedPassword",
            firstName = "John",
            lastName = "Doe",
            roles = listOf("USER"),
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = null,
            createdBy = "system",
            updatedBy = null
        )
    }

    // createUser() tests

    @Test
    fun `createUser returns created user when tenant matches and has permission`() = runBlocking {
        val tenantName = "tenantA"
        val mockTenant = createMockTenant(tenantName)
        val dto = CreateUserRequestDto(
            email = "u@t.com",
            phoneNumber = "5551234",
            password = "Password123!",
            firstName = "John",
            lastName = "Doe"
        )

        val domainUser = createMockUser(tenantName, dto.email, dto.phoneNumber)

        // Mock the security context
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true

        coEvery {
            userService.createAuthUser(
                email = dto.email,
                tenantName = tenantName,
                phoneNumber = dto.phoneNumber,
                password = dto.password,
                firstName = dto.firstName,
                lastName = dto.lastName,
                roles = listOf("USER")
            )
        } returns domainUser

        val response = userController.createUser(tenantName, dto)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(domainUser.toUserResponseDto(), response.body)
        coVerify(exactly = 1) {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER")
            userService.createAuthUser(
                email = dto.email,
                tenantName = tenantName,
                phoneNumber = dto.phoneNumber,
                password = dto.password,
                firstName = dto.firstName,
                lastName = dto.lastName,
                roles = listOf("USER")
            )
        }
    }

    @Test
    fun `createUser returns 401 when no authenticated user`() = runBlocking {
        val tenantName = "tenantA"
        val dto = CreateUserRequestDto("u@t.com", "555", "Password123!", null, null)

        // Mock no authenticated user
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns null

        val response = userController.createUser(tenantName, dto)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNull(response.body)
        coVerify(exactly = 1) { CoroutineSecurityUtils.getCurrentTenant() }
        confirmVerified(userService)
    }

    @Test
    fun `createUser returns 403 when token tenant mismatch`() = runBlocking {
        val tenantName = "tenantA"
        val mockTenant = createMockTenant("otherTenant")
        val dto = CreateUserRequestDto("u@t.com", "555", "Password123!", null, null)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant

        val response = userController.createUser(tenantName, dto)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNull(response.body)
        coVerify(exactly = 1) { CoroutineSecurityUtils.getCurrentTenant() }
        confirmVerified(userService)
    }

    @Test
    fun `createUser returns 403 when user lacks permission`() = runBlocking {
        val tenantName = "tenantA"
        val mockTenant = createMockTenant(tenantName)
        val dto = CreateUserRequestDto("u@t.com", "555", "Password123!", null, null)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns false

        val response = userController.createUser(tenantName, dto)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNull(response.body)
        coVerify(exactly = 1) {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER")
        }
        confirmVerified(userService)
    }

    @Test
    fun `createUser propagates service exception`() = runBlocking {
        val tenantName = "tenantA"
        val mockTenant = createMockTenant(tenantName)
        val dto = CreateUserRequestDto("u@t.com", "555", "Password123!", null, null)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true
        coEvery { userService.createAuthUser(any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("boom")

        val ex = assertThrows<RuntimeException> {
            runBlocking { userController.createUser(tenantName, dto) }
        }
        assertEquals("boom", ex.message)
        coVerify { CoroutineSecurityUtils.getCurrentTenant() }
        coVerify { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") }
    }

    // getUserByQueryParams() tests

    @Test
    fun `getUserByQueryParams returns 400 when both params null`() = runBlocking {
        val mockTenant = createMockTenant("t1")
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant

        val resp = userController.getUserByQueryParams("t1", null, null)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        assertNull(resp.body)
        coVerify { CoroutineSecurityUtils.getCurrentTenant() }
    }

    @Test
    fun `getUserByQueryParams returns 401 when no authenticated user`() = runBlocking {
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns null

        val resp = userController.getUserByQueryParams("t1", "e@x.com", null)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
        assertNull(resp.body)
    }

    @Test
    fun `getUserByQueryParams returns 403 when token tenant mismatch`() = runBlocking {
        val mockTenant = createMockTenant("other")
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant

        val resp = userController.getUserByQueryParams("t1", "e@x.com", null)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        assertNull(resp.body)
    }

    @Test
    fun `getUserByQueryParams returns user by email`() = runBlocking {
        val tenant = "t1"
        val email = "e@t.com"
        val mockTenant = createMockTenant(tenant)

        val domainUser = createMockUser(tenant, email, "555")

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { userService.getUserByEmail(email, tenant) } returns domainUser

        val resp = userController.getUserByQueryParams(tenant, email, null)

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(domainUser.toUserResponseDto(), resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            userService.getUserByEmail(email, tenant)
        }
    }

    @Test
    fun `getUserByQueryParams returns user by phone`() = runBlocking {
        val tenant = "t1"
        val phone = "5551234"
        val mockTenant = createMockTenant(tenant)

        val domainUser = createMockUser(tenant, "x@x.com", phone)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { userService.getUserByPhoneNumber(phone, tenant) } returns domainUser

        val resp = userController.getUserByQueryParams(tenant, null, phone)

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(domainUser.toUserResponseDto(), resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            userService.getUserByPhoneNumber(phone, tenant)
        }
    }

    @Test
    fun `getUserByQueryParams returns 404 when user not found`() = runBlocking {
        val tenant = "t1"
        val email = "no@no.com"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { userService.getUserByEmail(email, tenant) } returns null

        val resp = userController.getUserByQueryParams(tenant, email, null)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        assertNull(resp.body)
    }

    @Test
    fun `getUserByQueryParams propagates exception`() = runBlocking {
        val tenant = "t1"
        val email = "err@e.com"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { userService.getUserByEmail(email, tenant) } throws IllegalStateException("fail")

        val ex = assertThrows<IllegalStateException> {
            runBlocking { userController.getUserByQueryParams(tenant, email, null) }
        }
        assertEquals("fail", ex.message)
    }

    // deactivateUser() tests

    @Test
    fun `deactivateUser returns 400 when both params null`() = runBlocking {
        val mockTenant = createMockTenant("t1")
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true

        val resp = userController.deactivateUser("t1", null, null)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        assertEquals("Either email or phone number must be provided", resp.body)
    }

    @Test
    fun `deactivateUser returns 401 when no authenticated user`() = runBlocking {
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns null

        val resp = userController.deactivateUser("t1", "u@x.com", null)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
        assertNull(resp.body)
    }

    @Test
    fun `deactivateUser returns 403 when token tenant mismatch`() = runBlocking {
        val mockTenant = createMockTenant("other")
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant

        val resp = userController.deactivateUser("t1", "u@x.com", null)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        assertNull(resp.body)
    }

    @Test
    fun `deactivateUser returns 403 when user lacks permission`() = runBlocking {
        val mockTenant = createMockTenant("t1")
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns false

        val resp = userController.deactivateUser("t1", "u@x.com", null)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        assertNull(resp.body)

        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER")
        }
        confirmVerified(userService)
    }

    @Test
    fun `deactivateUser returns 200 when email success`() = runBlocking {
        val tenant = "t1"
        val email = "u@t.com"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true
        coEvery { userService.deactivateUserByEmail(email, tenant) } returns true

        val resp = userController.deactivateUser(tenant, email, null)

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("User deactivated successfully", resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER")
            userService.deactivateUserByEmail(email, tenant)
        }
    }

    @Test
    fun `deactivateUser returns 404 when email not found`() = runBlocking {
        val tenant = "t1"
        val email = "u@t.com"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true
        coEvery { userService.deactivateUserByEmail(email, tenant) } returns false

        val resp = userController.deactivateUser(tenant, email, null)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        assertNull(resp.body)
    }

    @Test
    fun `deactivateUser returns 200 when phone success`() = runBlocking {
        val tenant = "t1"
        val phone = "555"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true
        coEvery { userService.deactivateUserByPhoneNumber(phone, tenant) } returns true

        val resp = userController.deactivateUser(tenant, null, phone)

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("User deactivated successfully", resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER")
            userService.deactivateUserByPhoneNumber(phone, tenant)
        }
    }

    @Test
    fun `deactivateUser returns 404 when phone not found`() = runBlocking {
        val tenant = "t1"
        val phone = "000"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true
        coEvery { userService.deactivateUserByPhoneNumber(phone, tenant) } returns false

        val resp = userController.deactivateUser(tenant, null, phone)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        assertNull(resp.body)
    }

    @Test
    fun `deactivateUser propagates exception for email path`() = runBlocking {
        val tenant = "t1"
        val email = "u@t.com"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true
        coEvery { userService.deactivateUserByEmail(email, tenant) } throws IllegalStateException("oops")

        val ex = assertThrows<IllegalStateException> {
            runBlocking { userController.deactivateUser(tenant, email, null) }
        }
        assertEquals("oops", ex.message)
    }

    @Test
    fun `deactivateUser propagates exception for phone path`() = runBlocking {
        val tenant = "t1"
        val phone = "555"
        val mockTenant = createMockTenant(tenant)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns mockTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole("ADMIN", "USER_MANAGER") } returns true
        coEvery { userService.deactivateUserByPhoneNumber(phone, tenant) } throws IllegalStateException("boom")

        val ex = assertThrows<IllegalStateException> {
            runBlocking { userController.deactivateUser(tenant, null, phone) }
        }
        assertEquals("boom", ex.message)
    }

    // getCurrentUser() tests

    @Test
    fun `getCurrentUser returns current user when authenticated`() = runBlocking {
        val currentUser = createMockUser()
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser

        val resp = userController.getCurrentUser()

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(currentUser.toUserResponseDto(), resp.body)
        coVerify { CoroutineSecurityUtils.getCurrentUser() }
    }

    @Test
    fun `getCurrentUser returns 401 when not authenticated`() = runBlocking {
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns null

        val resp = userController.getCurrentUser()

        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
        assertNull(resp.body)
        coVerify { CoroutineSecurityUtils.getCurrentUser() }
    }

    // changePassword() tests

    @Test
    fun `changePassword returns 200 when successful`() = runBlocking {
        val tenantName = "t1"
        val currentUser = createMockUser(tenantName)
        val currentTenant = createMockTenant(tenantName)
        val request = ChangePasswordRequest("oldPass123!", "newPass456!")

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { userService.changePassword(currentUser.email, tenantName, "oldPass123!", "newPass456!") } returns true

        val resp = userController.changePassword(tenantName, request)

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("Password changed successfully", resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentUser()
            CoroutineSecurityUtils.getCurrentTenant()
            userService.changePassword(currentUser.email, tenantName, "oldPass123!", "newPass456!")
        }
    }

    @Test
    fun `changePassword returns 401 when user not authenticated`() = runBlocking {
        val request = ChangePasswordRequest("oldPass123!", "newPass456!")
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns null

        val resp = userController.changePassword("t1", request)

        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
        assertNull(resp.body)
        coVerify { CoroutineSecurityUtils.getCurrentUser() }
    }

    @Test
    fun `changePassword returns 401 when tenant not found`() = runBlocking {
        val currentUser = createMockUser()
        val request = ChangePasswordRequest("oldPass123!", "newPass456!")

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns null

        val resp = userController.changePassword("t1", request)

        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
        assertNull(resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentUser()
            CoroutineSecurityUtils.getCurrentTenant()
        }
    }

    @Test
    fun `changePassword returns 403 when tenant mismatch`() = runBlocking {
        val currentUser = createMockUser("tenantA")
        val currentTenant = createMockTenant("tenantB")
        val request = ChangePasswordRequest("oldPass123!", "newPass456!")

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant

        val resp = userController.changePassword("tenantA", request)

        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        assertNull(resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentUser()
            CoroutineSecurityUtils.getCurrentTenant()
        }
        confirmVerified(userService)
    }

    @Test
    fun `changePassword returns 400 when old password is invalid`() = runBlocking {
        val tenantName = "t1"
        val currentUser = createMockUser(tenantName)
        val currentTenant = createMockTenant(tenantName)
        val request = ChangePasswordRequest("wrongPass", "newPass456!")

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { userService.changePassword(currentUser.email, tenantName, "wrongPass", "newPass456!") } returns false

        val resp = userController.changePassword(tenantName, request)

        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        assertEquals("Invalid old password", resp.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentUser()
            CoroutineSecurityUtils.getCurrentTenant()
            userService.changePassword(currentUser.email, tenantName, "wrongPass", "newPass456!")
        }
    }

    @Test
    fun `changePassword propagates service exception`() = runBlocking {
        val tenantName = "t1"
        val currentUser = createMockUser(tenantName)
        val currentTenant = createMockTenant(tenantName)
        val request = ChangePasswordRequest("oldPass123!", "newPass456!")

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns currentUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { userService.changePassword(any(), any(), any(), any()) } throws RuntimeException("service error")

        val ex = assertThrows<RuntimeException> {
            runBlocking { userController.changePassword(tenantName, request) }
        }
        assertEquals("service error", ex.message)
        coVerify {
            CoroutineSecurityUtils.getCurrentUser()
            CoroutineSecurityUtils.getCurrentTenant()
        }
    }
}