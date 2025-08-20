package controller.v1

import com.muditsahni.controller.v1.TenantController
import com.muditsahni.error.TenantAlreadyExistsException
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.dto.request.CreateTenantRequestDto
import com.muditsahni.model.dto.response.toTenantResponseDto
import com.muditsahni.model.entity.Role
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.v1.DefaultTenantService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class TenantControllerTest {

    private lateinit var tenantService: DefaultTenantService
    private lateinit var tenantController: TenantController

    @BeforeEach
    fun setUp() {
        tenantService = mockk()
        tenantController = TenantController(tenantService)
        mockkObject(CoroutineSecurityUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CoroutineSecurityUtils)
    }

    // Helper method to create a mock tenant
    private fun createMockTenant(name: String = "testTenant"): Tenant {
        return Tenant(
            id = UUID.randomUUID(),
            name = name,
            type = "BUSINESS",
            createdAt = Instant.now(),
            createdBy = "admin@test.com",
            deleted = false,
            domains = setOf("example.com")
        )
    }

    // createTenant() tests

    @Test
    fun `createTenant returns created tenant when user has admin role`() = runBlocking {
        val domains = setOf("example.com", "test.com")
        val dto = CreateTenantRequestDto(name = "newTenant", type = "BUSINESS", domains = domains)
        val createdTenant = createMockTenant("newTenant")

        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.createTenant(dto.name, dto.type, domains) } returns createdTenant

        val response = tenantController.createTenant(dto)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(createdTenant.toTenantResponseDto(), response.body)
        coVerify {
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.createTenant(dto.name, dto.type, domains)
        }
    }

    @Test
    fun `createTenant returns created tenant when user has super admin role`() = runBlocking {
        val domains = setOf("example.com", "test.com")
        val dto = CreateTenantRequestDto(name = "newTenant", type = "BUSINESS", domains)
        val createdTenant = createMockTenant("newTenant")

        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.createTenant(dto.name, dto.type, domains) } returns createdTenant

        val response = tenantController.createTenant(dto)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(createdTenant.toTenantResponseDto(), response.body)
        coVerify {
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.createTenant(dto.name, dto.type, domains)
        }
    }

    @Test
    fun `createTenant returns 403 when user lacks admin role`() = runBlocking {
        val domains = setOf("example.com", "test.com")
        val dto = CreateTenantRequestDto(name = "newTenant", type = "BUSINESS", domains)

        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns false

        val response = tenantController.createTenant(dto)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNull(response.body)
        coVerify { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) }
        confirmVerified(tenantService)
    }

    @Test
    fun `createTenant propagates TenantAlreadyExistsException when tenant exists`() = runBlocking {
        val domains = setOf("example.com", "test.com")
        val dto = CreateTenantRequestDto(name = "existingTenant", type = "BUSINESS", domains)

        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.createTenant(dto.name, dto.type, domains) } throws TenantAlreadyExistsException(dto.name)

        val ex = assertThrows<TenantAlreadyExistsException> {
            runBlocking { tenantController.createTenant(dto) }
        }
        assertEquals("Tenant with name '${dto.name}' already exists", ex.message)
        coVerify {
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.createTenant(dto.name, dto.type, domains)
        }
    }

    @Test
    fun `createTenant propagates service exception`() = runBlocking {
        val domains = setOf("example.com", "test.com")

        val dto = CreateTenantRequestDto(name = "newTenant", type = "BUSINESS", domains)

        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.createTenant(dto.name, dto.type, domains) } throws RuntimeException("Service error")

        val ex = assertThrows<RuntimeException> {
            runBlocking { tenantController.createTenant(dto) }
        }
        assertEquals("Service error", ex.message)
        coVerify {
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.createTenant(dto.name, dto.type, domains)
        }
    }

    // getTenantByName() tests

    @Test
    fun `getTenantByName returns tenant when user is admin and tenant exists`() = runBlocking {
        val tenantName = "testTenant"
        val mockTenant = createMockTenant(tenantName)
        val currentTenant = createMockTenant("userTenant")

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.getTenantByName(tenantName) } returns mockTenant

        val response = tenantController.getTenantByName(tenantName)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(mockTenant.toTenantResponseDto(), response.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.getTenantByName(tenantName)
        }
    }

    @Test
    fun `getTenantByName returns own tenant when user is not admin`() = runBlocking {
        val tenantName = "userTenant"
        val currentTenant = createMockTenant(tenantName)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns false
        coEvery { tenantService.getTenantByName(tenantName) } returns currentTenant

        val response = tenantController.getTenantByName(tenantName)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(currentTenant.toTenantResponseDto(), response.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.getTenantByName(tenantName)
        }
    }

    @Test
    fun `getTenantByName returns 401 when no authenticated user`() = runBlocking {
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns null

        val response = tenantController.getTenantByName("testTenant")

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNull(response.body)
        coVerify { CoroutineSecurityUtils.getCurrentTenant() }
        confirmVerified(tenantService)
    }

    @Test
    fun `getTenantByName returns 403 when user tries to access different tenant`() = runBlocking {
        val requestedTenantName = "otherTenant"
        val currentTenant = createMockTenant("userTenant")

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns false

        val response = tenantController.getTenantByName(requestedTenantName)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNull(response.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
        }
        confirmVerified(tenantService)
    }

    @Test
    fun `getTenantByName returns 404 when tenant not found`() = runBlocking {
        val tenantName = "nonExistentTenant"
        val currentTenant = createMockTenant(tenantName)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns false
        coEvery { tenantService.getTenantByName(tenantName) } returns null

        val response = tenantController.getTenantByName(tenantName)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNull(response.body)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.getTenantByName(tenantName)
        }
    }

    @Test
    fun `getTenantByName propagates service exception`() = runBlocking {
        val tenantName = "errorTenant"
        val currentTenant = createMockTenant(tenantName)

        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns currentTenant
        coEvery { CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN) } returns false
        coEvery { tenantService.getTenantByName(tenantName) } throws IllegalStateException("Service error")

        val ex = assertThrows<IllegalStateException> {
            runBlocking { tenantController.getTenantByName(tenantName) }
        }
        assertEquals("Service error", ex.message)
        coVerify {
            CoroutineSecurityUtils.getCurrentTenant()
            CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
            tenantService.getTenantByName(tenantName)
        }
    }

    // deleteTenantByName() tests

    @Test
    fun `deleteTenantByName returns success when user is super admin`() = runBlocking {
        val tenantName = "tenantToDelete"

        coEvery { CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.deleteTenant(tenantName) } returns true

        val response = tenantController.deleteTenantByName(tenantName)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Tenant $tenantName deleted successfully", response.body)
        coVerify {
            CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN)
            tenantService.deleteTenant(tenantName)
        }
    }

    @Test
    fun `deleteTenantByName returns 403 when user is not super admin`() = runBlocking {
        val tenantName = "tenantToDelete"

        coEvery { CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN) } returns false

        val response = tenantController.deleteTenantByName(tenantName)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNull(response.body)
        coVerify { CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN) }
        confirmVerified(tenantService)
    }

    @Test
    fun `deleteTenantByName returns 404 when tenant not found`() = runBlocking {
        val tenantName = "nonExistentTenant"

        coEvery { CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.deleteTenant(tenantName) } returns false

        val response = tenantController.deleteTenantByName(tenantName)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNull(response.body)
        coVerify {
            CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN)
            tenantService.deleteTenant(tenantName)
        }
    }

    @Test
    fun `deleteTenantByName propagates service exception`() = runBlocking {
        val tenantName = "errorTenant"

        coEvery { CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN) } returns true
        coEvery { tenantService.deleteTenant(tenantName) } throws RuntimeException("Service error")

        val ex = assertThrows<RuntimeException> {
            runBlocking { tenantController.deleteTenantByName(tenantName) }
        }
        assertEquals("Service error", ex.message)
        coVerify {
            CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN)
            tenantService.deleteTenant(tenantName)
        }
    }
}