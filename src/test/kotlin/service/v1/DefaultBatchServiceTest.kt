package com.muditsahni.service.v1

import com.muditsahni.constant.General
import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import com.muditsahni.model.entity.Tenant
import com.muditsahni.repository.BatchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultBatchServiceTest {

    private lateinit var batchRepository: BatchRepository
    private lateinit var batchService: DefaultBatchService
    private lateinit var tenant: Tenant

    @BeforeEach
    fun setup() {
        tenant = Tenant(name = "tenant-123", type = "default", createdBy = General.SYSTEM.toString(), domains = setOf("example.com"))
        batchRepository = mockk()
        batchService = DefaultBatchService(batchRepository)
    }

    @Test
    fun `createBatch should create and save a batch with correct properties`() = runBlocking {
        // Arrange
        val batchName = "Test Batch"
        val batchType = BatchType.INVOICE
        val user = General.SYSTEM.toString()

        val batchSlot = slot<Batch>()
        val savedBatch = Batch(
            name = batchName,
            tenantName = tenant.name,
            type = batchType,
            status = BatchStatus.CREATED,
            createdBy = user,
            owners = listOf(user)
        )

        coEvery { batchRepository.save(capture(batchSlot)) } returns savedBatch

        // Act
        val result = batchService.createBatch(tenant, batchName, batchType)

        // Assert
        assertEquals(savedBatch, result)

        val capturedBatch = batchSlot.captured
        assertEquals(batchName, capturedBatch.name)
        assertEquals(batchType, capturedBatch.type)
        assertEquals(BatchStatus.CREATED, capturedBatch.status)
        assertEquals(user, capturedBatch.createdBy)
        assertEquals(listOf(user), capturedBatch.owners)

        coVerify(exactly = 1) { batchRepository.save(any()) }
    }
}