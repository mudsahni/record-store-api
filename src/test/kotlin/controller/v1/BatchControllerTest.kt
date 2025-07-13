//package com.muditsahni.controller.v1
//
//import com.muditsahni.error.InvalidRequestException
//import com.muditsahni.model.dto.request.CreateBatchRequestDto
//import com.muditsahni.model.entity.Batch
//import com.muditsahni.model.entity.BatchStatus
//import com.muditsahni.model.entity.BatchType
//import com.muditsahni.service.v1.DefaultBatchService
//import io.mockk.coEvery
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//import org.springframework.http.HttpStatus
//import java.time.Instant
//
//class BatchControllerTest {
//
//    private lateinit var batchService: DefaultBatchService
//    private lateinit var batchController: BatchController
//
//    @BeforeEach
//    fun setup() {
//        batchService = mockk()
//        batchController = BatchController(batchService)
//    }
//
//    @Test
//    fun `createBatch should return 200 OK with correct response when valid request is provided`() = runBlocking {
//        // Arrange
//        val tenantId = "tenant-123"
//        val batchName = "Test Batch"
//        val batchType = BatchType.INVOICE
//
//        val createBatchRequest = CreateBatchRequestDto(
//            name = batchName,
//            type = batchType
//        )
//
//        val createdBatch = Batch(
//            name = batchName,
//            type = batchType,
//            status = BatchStatus.CREATED,
//            createdBy = "SYSTEM",
//            owners = listOf("SYSTEM"),
//            createdAt = Instant.now(),
//            updatedAt = Instant.now()
//        )
//
//        coEvery {
//            batchService.createBatch(tenantId, batchName, batchType)
//        } returns createdBatch
//
//        // Act
//        val response = batchController.createBatch(tenantId, createBatchRequest)
//
//        // Assert
//        assertEquals(HttpStatus.OK.value(), response.statusCodeValue)
//        assertEquals(createdBatch.id, response.body?.id)
//        assertEquals(createdBatch.name, response.body?.name)
//        assertEquals(createdBatch.type, response.body?.type)
//        assertEquals(createdBatch.status, response.body?.status)
//
//        coVerify(exactly = 1) { batchService.createBatch(tenantId, batchName, batchType) }
//    }
//
//    @Test
//    fun `createBatch should throw InvalidRequestException when batch name is empty`() = runBlocking {
//        // Arrange
//        val tenantId = "tenant-123"
//        val createBatchRequest = CreateBatchRequestDto(
//            name = "",
//            type = BatchType.INVOICE
//        )
//
//        // Act & Assert
//        val exception = assertThrows<InvalidRequestException> {
//            batchController.createBatch(tenantId, createBatchRequest)
//        }
//
//        assertEquals("Batch name cannot be empty", exception.message)
//
//        coVerify(exactly = 0) { batchService.createBatch(any(), any(), any()) }
//    }
//
//    @Test
//    fun `createBatch should throw InvalidRequestException when batch name is blank`() = runBlocking {
//        // Arrange
//        val tenantId = "tenant-123"
//        val createBatchRequest = CreateBatchRequestDto(
//            name = "   ",
//            type = BatchType.INVOICE
//        )
//
//        // Act & Assert
//        val exception = assertThrows<InvalidRequestException> {
//            batchController.createBatch(tenantId, createBatchRequest)
//        }
//
//        assertEquals("Batch name cannot be empty", exception.message)
//
//        coVerify(exactly = 0) { batchService.createBatch(any(), any(), any()) }
//    }
//
//    @Test
//    fun `createBatch should propagate exceptions from batchService`() = runBlocking {
//        // Arrange
//        val tenantId = "tenant-123"
//        val batchName = "Test Batch"
//        val batchType = BatchType.INVOICE
//
//        val createBatchRequest = CreateBatchRequestDto(
//            name = batchName,
//            type = batchType
//        )
//
//        val expectedException = RuntimeException("Service error")
//
//        coEvery {
//            batchService.createBatch(tenantId, batchName, batchType)
//        } throws expectedException
//
//        // Act & Assert
//        val exception = assertThrows<RuntimeException> {
//            batchController.createBatch(tenantId, createBatchRequest)
//        }
//
//        assertEquals("Service error", exception.message)
//
//        coVerify(exactly = 1) { batchService.createBatch(tenantId, batchName, batchType) }
//    }
//}