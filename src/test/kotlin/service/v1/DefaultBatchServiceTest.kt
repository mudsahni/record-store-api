package service.v1

import com.muditsahni.model.entity.*
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.repository.BatchRepository
import com.muditsahni.service.v1.DefaultBatchService
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.util.*

class DefaultBatchServiceTest {

    private lateinit var batchRepository: BatchRepository
    private lateinit var batchService: DefaultBatchService
    private lateinit var testTenant: Tenant
    private lateinit var testUser: User
    private lateinit var testBatch: Batch

    @BeforeEach
    fun setup() {
        batchRepository = mockk()
        batchService = DefaultBatchService(batchRepository)

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

        testBatch = Batch(
            id = UUID.randomUUID(),
            name = "Test Batch",
            tenantName = testTenant.name,
            type = BatchType.INVOICE,
            status = BatchStatus.CREATED,
            createdBy = testUser.email,
            owners = listOf(testUser.email),
            tags = mapOf("environment" to "test"),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    @Test
    fun `createBatch should create and save a batch with correct properties`() = runTest {
        // Given
        val batchName = "Test Batch"
        val batchType = BatchType.INVOICE
        val tags = mapOf("environment" to "test", "priority" to "high")

        val batchSlot = slot<Batch>()
        coEvery { batchRepository.save(capture(batchSlot)) } returns testBatch

        // When
        val result = batchService.createBatch(testTenant, batchName, batchType, tags, testUser)

        // Then
        assertEquals(testBatch, result)

        val capturedBatch = batchSlot.captured
        assertEquals(batchName, capturedBatch.name)
        assertEquals(testTenant.name, capturedBatch.tenantName)
        assertEquals(batchType, capturedBatch.type)
        assertEquals(BatchStatus.CREATED, capturedBatch.status)
        assertEquals(testUser.email, capturedBatch.createdBy)
        assertEquals(listOf(testUser.email), capturedBatch.owners)
        assertEquals(tags, capturedBatch.tags)

        coVerify(exactly = 1) { batchRepository.save(any()) }
    }

    @Test
    fun `createBatch should handle empty tags`() = runTest {
        // Given
        val batchName = "Test Batch"
        val batchType = BatchType.INVOICE
        val emptyTags = emptyMap<String, String>()

        val batchSlot = slot<Batch>()
        val expectedBatch = testBatch.copy(tags = emptyTags)
        coEvery { batchRepository.save(capture(batchSlot)) } returns expectedBatch

        // When
        val result = batchService.createBatch(testTenant, batchName, batchType, emptyTags, testUser)

        // Then
        assertEquals(expectedBatch, result)
        assertEquals(emptyTags, batchSlot.captured.tags)

        coVerify(exactly = 1) { batchRepository.save(any()) }
    }

    @Test
    fun `getAllBatches should return flow of all batches`() = runTest {
        // Given
        val batches = listOf(testBatch, testBatch.copy(id = UUID.randomUUID(), name = "Another Batch"))
        coEvery { batchRepository.findAll() } returns flowOf(*batches.toTypedArray())

        // When
        val result = batchService.getAllBatches().toList()

        // Then
        assertEquals(batches, result)
        coVerify(exactly = 1) { batchRepository.findAll() }
    }

    @Test
    fun `getBatchesByStatus should return batches with specified status`() = runTest {
        // Given
        val status = BatchStatus.PARSING
        val processingBatches = listOf(
            testBatch.copy(status = BatchStatus.PARSING),
            testBatch.copy(id = UUID.randomUUID(), status = BatchStatus.PARSING)
        )
        coEvery { batchRepository.findByStatus(status) } returns flowOf(*processingBatches.toTypedArray())

        // When
        val result = batchService.getBatchesByStatus(status).toList()

        // Then
        assertEquals(processingBatches, result)
        coVerify(exactly = 1) { batchRepository.findByStatus(status) }
    }

    @Test
    fun `getBatchesCreatedByUser should return batches created by specific user`() = runTest {
        // Given
        val userEmail = testUser.email
        val userBatches = listOf(
            testBatch,
            testBatch.copy(id = UUID.randomUUID(), name = "User's Second Batch")
        )
        coEvery { batchRepository.findByCreatedBy(userEmail) } returns flowOf(*userBatches.toTypedArray())

        // When
        val result = batchService.getBatchesCreatedByUser(userEmail).toList()

        // Then
        assertEquals(userBatches, result)
        coVerify(exactly = 1) { batchRepository.findByCreatedBy(userEmail) }
    }

    @Test
    fun `getBatchById should return batch when found`() = runTest {
        // Given
        val batchId = testBatch.id!!
        coEvery { batchRepository.findById(batchId) } returns testBatch

        // When
        val result = batchService.getBatchById(batchId)

        // Then
        assertEquals(testBatch, result)
        coVerify(exactly = 1) { batchRepository.findById(batchId) }
    }

    @Test
    fun `getBatchById should return null when batch not found`() = runTest {
        // Given
        val batchId = UUID.randomUUID()
        coEvery { batchRepository.findById(batchId) } returns null

        // When
        val result = batchService.getBatchById(batchId)

        // Then
        assertNull(result)
        coVerify(exactly = 1) { batchRepository.findById(batchId) }
    }

    @Test
    fun `updateBatchStatus should update existing batch status`() = runTest {
        // Given
        val batchId = testBatch.id
        val newStatus = BatchStatus.UPLOADED
        val updatingUser = testUser.copy(email = "updater@example.com")

        val updatedBatch = testBatch.copy(
            status = newStatus,
            updatedBy = updatingUser.email,
            updatedAt = Instant.now()
        )

        coEvery { batchRepository.findById(batchId) } returns testBatch
        coEvery { batchRepository.save(any()) } returns updatedBatch

        // When
        val result = batchService.updateBatchStatus(batchId, newStatus, updatingUser)

        // Then
        assertNotNull(result)
        assertEquals(newStatus, result!!.status)
        assertEquals(updatingUser.email, result.updatedBy)
        assertNotNull(result.updatedAt)

        coVerify(exactly = 1) { batchRepository.findById(batchId) }
        coVerify(exactly = 1) { batchRepository.save(any()) }
    }

    @Test
    fun `updateBatchStatus should return null when batch not found`() = runTest {
        // Given
        val batchId = UUID.randomUUID()
        val newStatus = BatchStatus.UPLOADED
        val updatingUser = testUser

        coEvery { batchRepository.findById(batchId) } returns null

        // When
        val result = batchService.updateBatchStatus(batchId, newStatus, updatingUser)

        // Then
        assertNull(result)
        coVerify(exactly = 1) { batchRepository.findById(batchId) }
        coVerify(exactly = 0) { batchRepository.save(any()) }
    }

    @Test
    fun `updateBatchStatus should preserve existing batch data while updating status`() = runTest {
        // Given
        val batchId = testBatch.id
        val newStatus = BatchStatus.UPLOADED
        val updatingUser = testUser

        val batchSlot = slot<Batch>()
        val updatedBatch = testBatch.copy(status = newStatus, updatedBy = updatingUser.email)

        coEvery { batchRepository.findById(batchId) } returns testBatch
        coEvery { batchRepository.save(capture(batchSlot)) } returns updatedBatch

        // When
        val result = batchService.updateBatchStatus(batchId, newStatus, updatingUser)

        // Then
        val capturedBatch = batchSlot.captured
        assertEquals(testBatch.id, capturedBatch.id)
        assertEquals(testBatch.name, capturedBatch.name)
        assertEquals(testBatch.tenantName, capturedBatch.tenantName)
        assertEquals(testBatch.type, capturedBatch.type)
        assertEquals(testBatch.createdBy, capturedBatch.createdBy)
        assertEquals(testBatch.owners, capturedBatch.owners)
        assertEquals(testBatch.tags, capturedBatch.tags)
        assertEquals(newStatus, capturedBatch.status)
        assertEquals(updatingUser.email, capturedBatch.updatedBy)
        assertNotNull(capturedBatch.updatedAt)
    }

    @ParameterizedTest
    @EnumSource(BatchType::class)
    fun `createBatch should handle all batch types correctly`(batchType: BatchType) = runTest {
        // Given
        val slot = slot<Batch>()
        coEvery { batchRepository.save(capture(slot)) } returns testBatch.copy(type = batchType)

        // When
        val result = batchService.createBatch(
            testTenant,
            "Test Batch for ${batchType.name}",
            batchType,
            emptyMap(),
            testUser
        )

        // Then
        val capturedBatch = slot.captured
        assertEquals(batchType, capturedBatch.type)
        assertEquals("Test Batch for ${batchType.name}", capturedBatch.name)
        assertEquals(testTenant.name, capturedBatch.tenantName)
        assertEquals(BatchStatus.CREATED, capturedBatch.status)
        assertEquals(testUser.email, capturedBatch.createdBy)

        coVerify(exactly = 1) { batchRepository.save(any()) }
    }
    @Test
    fun `createBatch should handle different users correctly`() = runTest {
        // Given
        val anotherUser = testUser.copy(
            id = UUID.randomUUID(),
            email = "another.user@example.com",
            firstName = "Jane",
            lastName = "Smith"
        )

        val slot = slot<Batch>()
        coEvery { batchRepository.save(capture(slot)) } returns testBatch.copy(
            createdBy = anotherUser.email,
            owners = listOf(anotherUser.email)
        )

        // When
        val result = batchService.createBatch(
            testTenant,
            "Test Batch",
            BatchType.INVOICE,
            emptyMap(),
            anotherUser
        )

        // Then
        val capturedBatch = slot.captured
        assertEquals(anotherUser.email, capturedBatch.createdBy)
        assertEquals(listOf(anotherUser.email), capturedBatch.owners)
        assertEquals("Test Batch", capturedBatch.name)
        assertEquals(testTenant.name, capturedBatch.tenantName)

        coVerify(exactly = 1) { batchRepository.save(any()) }
    }
}