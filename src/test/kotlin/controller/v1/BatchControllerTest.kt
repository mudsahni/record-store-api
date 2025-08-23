package controller.v1

import com.muditsahni.config.TenantContext
import com.muditsahni.controller.v1.BatchController
import com.muditsahni.error.InvalidRequestException
import com.muditsahni.model.dto.request.CreateBatchRequestDto
import com.muditsahni.model.entity.*
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.v1.DefaultBatchService
import com.muditsahni.service.v1.DefaultTenantService
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.*

class BatchControllerTest {

    private lateinit var batchService: DefaultBatchService
    private lateinit var tenantService: DefaultTenantService
    private lateinit var batchController: BatchController

    private lateinit var testUser: User
    private lateinit var testTenant: Tenant
    private lateinit var adminUser: User

    @BeforeEach
    fun setup() {
        batchService = mockk()
        tenantService = mockk()
        batchController = BatchController(tenantService, batchService)

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

        adminUser = testUser.copy(
            id = UUID.randomUUID(),
            email = "admin@example.com",
            roles = listOf(Role.ADMIN)
        )

        // Mock static objects with proper CoroutineContext.Element implementation
        mockkObject(TenantContext)
        mockkObject(CoroutineSecurityUtils)

        every { TenantContext.setTenant(any<Tenant>()) } answers {
            TenantContext.TenantContextElement(firstArg())
        }
    }

    @Test
    fun `createBatch should return 201 CREATED with correct response when valid request is provided`() = runBlocking {
        // Arrange
        val batchName = "Test Batch"
        val batchType = BatchType.INVOICE
        val tags = mapOf("environment" to "test")

        val createBatchRequest = CreateBatchRequestDto(
            name = batchName,
            type = batchType,
            tags = tags
        )

        val createdBatch = Batch(
            id = UUID.randomUUID(),
            name = batchName,
            tenantName = testTenant.name,
            type = batchType,
            status = BatchStatus.CREATED,
            createdBy = testUser.email,
            owners = listOf(testUser.email),
            tags = tags,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Mock security context
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant

        // Mock service call
        coEvery {
            batchService.createBatch(testTenant, batchName, batchType, tags, testUser)
        } returns createdBatch

        // Act
        val response = batchController.createBatch(createBatchRequest)

        // Assert
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(createdBatch.id, response.body?.id)
        assertEquals(createdBatch.name, response.body?.name)
        assertEquals(createdBatch.type, response.body?.type)
        assertEquals(createdBatch.status, response.body?.status)

        coVerify(exactly = 1) { batchService.createBatch(testTenant, batchName, batchType, tags, testUser) }
        verify { TenantContext.setTenant(testTenant) }
    }

    @Test
    fun `createBatch should return 401 UNAUTHORIZED when user not authenticated`() = runBlocking {
        // Arrange
        val createBatchRequest = CreateBatchRequestDto(
            name = "Test Batch",
            type = BatchType.INVOICE,
            tags = emptyMap()
        )

        // Mock no user authenticated
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns null

        // Act
        val response = batchController.createBatch(createBatchRequest)

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        coVerify(exactly = 0) { batchService.createBatch(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `createBatch should return 401 UNAUTHORIZED when tenant not available`() = runBlocking {
        // Arrange
        val createBatchRequest = CreateBatchRequestDto(
            name = "Test Batch",
            type = BatchType.INVOICE,
            tags = emptyMap()
        )

        // Mock user but no tenant
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns null

        // Act
        val response = batchController.createBatch(createBatchRequest)

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        coVerify(exactly = 0) { batchService.createBatch(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `createBatch should throw InvalidRequestException when batch name is empty`() = runBlocking {
        // Arrange
        val createBatchRequest = CreateBatchRequestDto(
            name = "",
            type = BatchType.INVOICE,
            tags = emptyMap()
        )

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant

        // Act & Assert
        val exception = assertThrows<InvalidRequestException> {
            batchController.createBatch(createBatchRequest)
        }

        assertEquals("Batch name cannot be empty", exception.message)
        coVerify(exactly = 0) { batchService.createBatch(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `createBatch should throw InvalidRequestException when batch name is blank`() = runBlocking {
        // Arrange
        val createBatchRequest = CreateBatchRequestDto(
            name = "   ",
            type = BatchType.INVOICE,
            tags = emptyMap()
        )

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant

        // Act & Assert
        val exception = assertThrows<InvalidRequestException> {
            batchController.createBatch(createBatchRequest)
        }

        assertEquals("Batch name cannot be empty", exception.message)
        coVerify(exactly = 0) { batchService.createBatch(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `createBatch should propagate exceptions from batchService`() = runBlocking {
        // Arrange
        val batchName = "Test Batch"
        val batchType = BatchType.INVOICE
        val tags = emptyMap<String, String>()

        val createBatchRequest = CreateBatchRequestDto(
            name = batchName,
            type = batchType,
            tags = tags
        )

        val expectedException = RuntimeException("Service error")

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant
        coEvery {
            batchService.createBatch(testTenant, batchName, batchType, tags, testUser)
        } throws expectedException

        // Act & Assert
        val exception = assertThrows<RuntimeException> {
            batchController.createBatch(createBatchRequest)
        }

        assertEquals("Service error", exception.message)
        coVerify(exactly = 1) { batchService.createBatch(testTenant, batchName, batchType, tags, testUser) }
    }

    @Test
    fun `createBatch should handle different batch types`() = runBlocking {
        // Arrange
        val batchTypes = listOf(BatchType.INVOICE, BatchType.EXPENSE)

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant

        batchTypes.forEach { batchType ->
            // Clear previous mocks to avoid interference
            clearMocks(batchService, answers = false)

            val createBatchRequest = CreateBatchRequestDto(
                name = "Test Batch",
                type = batchType,
                tags = emptyMap()
            )

            val createdBatch = Batch(
                id = UUID.randomUUID(),
                name = "Test Batch",
                tenantName = testTenant.name,
                type = batchType,
                status = BatchStatus.CREATED,
                createdBy = testUser.email,
                owners = listOf(testUser.email),
                tags = emptyMap(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            coEvery {
                batchService.createBatch(testTenant, "Test Batch", batchType, emptyMap(), testUser)
            } returns createdBatch

            // Act
            val response = batchController.createBatch(createBatchRequest)

            // Assert
            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertEquals(batchType, response.body?.type)
        }
    }

    @Test
    fun `getAllBatches should return 200 OK with batches for admin user`() = runBlocking {
        // Arrange
        val batches = listOf(
            Batch(
                id = UUID.randomUUID(),
                name = "Batch 1",
                tenantName = testTenant.name,
                type = BatchType.INVOICE,
                status = BatchStatus.CREATED,
                createdBy = testUser.email,
                owners = listOf(testUser.email),
                tags = emptyMap(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ),
            Batch(
                id = UUID.randomUUID(),
                name = "Batch 2",
                tenantName = testTenant.name,
                type = BatchType.EXPENSE,
                status = BatchStatus.PARSING,
                createdBy = testUser.email,
                owners = listOf(testUser.email),
                tags = emptyMap(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns adminUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant
        coEvery { batchService.getAllBatches() } returns flowOf(*batches.toTypedArray())

        // Act
        val response = batchController.getAllBatches()

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(2, response.body!!.size)
        assertEquals("Batch 1", response.body!![0].name)
        assertEquals("Batch 2", response.body!![1].name)

        coVerify(exactly = 1) { batchService.getAllBatches() }
        verify { TenantContext.setTenant(testTenant) }
    }

    @Test
    fun `getAllBatches should return 401 UNAUTHORIZED when user not authenticated`() = runBlocking {
        // Arrange
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns null

        // Act
        val response = batchController.getAllBatches()

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        coVerify(exactly = 0) { batchService.getAllBatches() }
    }

    @Test
    fun `getAllBatches should return 403 FORBIDDEN when user is not admin`() = runBlocking {
        // Arrange
        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns testUser // Regular user
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant

        // Act
        val response = batchController.getAllBatches()

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        coVerify(exactly = 0) { batchService.getAllBatches() }
    }

    @Test
    fun `getAllBatches should work for SUPER_ADMIN user`() = runBlocking {
        // Arrange
        val superAdminUser = testUser.copy(
            email = "superadmin@example.com",
            roles = listOf(Role.SUPER_ADMIN)
        )

        val batch = Batch(
            id = UUID.randomUUID(),
            name = "Test Batch",
            tenantName = testTenant.name,
            type = BatchType.INVOICE,
            status = BatchStatus.CREATED,
            createdBy = testUser.email,
            owners = listOf(testUser.email),
            tags = emptyMap(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { CoroutineSecurityUtils.getCurrentUser() } returns superAdminUser
        coEvery { CoroutineSecurityUtils.getCurrentTenant() } returns testTenant
        coEvery { batchService.getAllBatches() } returns flowOf(batch)

        // Act
        val response = batchController.getAllBatches()

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body!!.size)
        coVerify { batchService.getAllBatches() }
    }
}