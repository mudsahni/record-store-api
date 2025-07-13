package service.v1

import com.muditsahni.constant.General
import com.muditsahni.error.TenantAlreadyExistsException
import com.muditsahni.model.entity.Tenant
import com.muditsahni.repository.TenantRepository
import com.muditsahni.service.v1.DefaultTenantService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class DefaultTenantServiceTest {

    private lateinit var tenantRepository: TenantRepository
    private lateinit var tenantService: DefaultTenantService

    @BeforeEach
    fun setup() {
        tenantRepository = mockk()
        tenantService = DefaultTenantService(tenantRepository)
    }

    @Test
    fun `createTenant should save tenant with correct properties`() = runBlocking {
        // Arrange
        val tenantName = "Test Tenant"
        val tenantType = "default"
        val now        = Instant.now()
        val savedTenant = Tenant(
            id        = UUID.randomUUID(),
            name      = tenantName,
            type      = tenantType,
            createdAt = now,
            updatedAt = null,
            deleted   = false,
            createdBy = General.SYSTEM.toString()
        )

        coEvery { tenantRepository.findByName(tenantName) } returns null
        coEvery { tenantRepository.save(any()) } returns savedTenant

        // Act
        val result = tenantService.createTenant(tenantName, tenantType)

        // Assert
        assertEquals(savedTenant, result)
        coVerify(exactly = 1) { tenantRepository.findByName(tenantName) }
        coVerify(exactly = 1) { tenantRepository.save(match {
            it.name == tenantName && it.type == tenantType
        }) }
    }

    @Test
    fun `getTenantByName should return tenant by name`() = runBlocking {
        // Arrange
        val tenantName = "Test Tenant"
        val expectedTenant = Tenant(name = tenantName, type = "default", createdBy = General.SYSTEM.toString())
        coEvery { tenantRepository.findByName(tenantName) } returns expectedTenant

        // Act
        val result = tenantService.getTenantByName(tenantName)

        // Assert
        assertEquals(expectedTenant, result)
        coVerify { tenantRepository.findByName(tenantName) }
    }

    @Test
    fun `getTenantByName returns tenant when not deleted`() = runBlocking {
        // Arrange
        val tenantName = "Live Tenant"
        val now = Instant.now()
        val tenant = Tenant(
            id = UUID.randomUUID(),
            name = tenantName,
            type = "typeA",
            createdAt = now,
            updatedAt = null,
            deleted = false,
            createdBy = General.SYSTEM.toString()
        )
        coEvery { tenantRepository.findByName(tenantName) }
            .returns(tenant)

        // Act
        val result = tenantService.getTenantByName(tenantName)

        // Assert
        assertNotNull(result)
        assertEquals(tenant, result)
        coVerify { tenantRepository.findByName(tenantName) }
    }

    @Test
    fun `getTenantByName returns null when tenant is soft-deleted`() = runBlocking {
        // Arrange
        val tenantName = "Deleted Tenant"
        val now = Instant.now()
        val tenant = Tenant(
            id = UUID.randomUUID(),
            name = tenantName,
            type = "typeB",
            createdAt = now,
            updatedAt = now,
            deleted = true,
            createdBy = General.SYSTEM.toString()
        )
        coEvery { tenantRepository.findByName(tenantName) }
            .returns(tenant)

        // Act
        val result = tenantService.getTenantByName(tenantName)

        // Assert
        assertNull(result)
        coVerify { tenantRepository.findByName(tenantName) }
    }

    @Test
    fun `getTenantByName returns null when no tenant found`() = runBlocking {
        // Arrange
        val tenantName = "Unknown Tenant"
        coEvery { tenantRepository.findByName(tenantName) }
            .returns(null)

        // Act
        val result = tenantService.getTenantByName(tenantName)

        // Assert
        assertNull(result)
        coVerify { tenantRepository.findByName(tenantName) }
    }

    @Test
    fun `createTenant throws TenantAlreadyExistsException when name already exists`() {
        runBlocking {
            // Arrange
            val tenantName = "Existing Tenant"
            val tenantType = "default"
            val existing = Tenant(
                id        = UUID.randomUUID(),
                name      = tenantName,
                type      = tenantType,
                createdAt = Instant.now(),
                updatedAt = null,
                deleted   = false,
                createdBy = General.SYSTEM.toString()
            )
            coEvery { tenantRepository.findByName(tenantName) } returns existing

            // Act & Assert
            val ex = assertThrows<TenantAlreadyExistsException> {
                runBlocking {
                    tenantService.createTenant(tenantName, tenantType)
                }
            }
            assertEquals("Tenant with name '$tenantName' already exists", ex.message)

            // verify we never call save in the duplicate case
            coVerify(exactly = 1) { tenantRepository.findByName(tenantName) }
            coVerify(exactly = 0) { tenantRepository.save(any()) }
        }
    }
}