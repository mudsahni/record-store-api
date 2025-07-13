//package com.muditsahni.controller.v1
//
//import io.mockk.Runs
//import io.mockk.coEvery
//import io.mockk.mockk
//import io.mockk.coVerify
//import io.mockk.just
//import kotlinx.coroutines.runBlocking
//import com.muditsahni.service.command.UploadRequestCommand
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.springframework.http.HttpStatus
//import java.util.UUID
//import org.assertj.core.api.Assertions.assertThat
//import com.muditsahni.error.RecordNotFoundException
//import com.muditsahni.error.StorageServiceException
//import com.muditsahni.models.dto.request.UploadRequestDto
//import com.muditsahni.models.entity.RecordType
//import com.muditsahni.service.command.UploadRequestResult
//import com.muditsahni.service.v1.DefaultRecordService
//import org.junit.jupiter.api.assertThrows
//
//class RecordControllerTest {
//
//    private lateinit var recordService: DefaultRecordService
//    private lateinit var recordController: RecordController
//
//    @BeforeEach
//    fun setup() {
//        recordService = mockk(relaxed = true)
//        recordController = RecordController(recordService)
//    }
//
//    @Test
//    fun `requestUpload should return successful response with upload URL`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//        val uploadUrl = "https://example.blob.core.windows.net/container/records/${recordId}/file.pdf?sig=signature"
//
//        val uploadRequestDto = UploadRequestDto(
//            batchId = UUID.randomUUID(),
//            fileName = "file.pdf",
//            type = RecordType.INVOICE,
//            createdBy = "user1"
//        )
//
//        val serviceResult = UploadRequestResult(
//            recordId = recordId,
//            uploadUrl = uploadUrl
//        )
//
//        // Mock service call with any() for the command parameter
//        coEvery {
//            recordService.requestUpload(eq(tenantId), any())
//        } returns serviceResult
//
//        // When
//        val response = recordController.requestUpload(tenantId, uploadRequestDto)
//
//        // Then
//        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
//        assertThat(response.body?.recordId).isEqualTo(recordId)
//        assertThat(response.body?.uploadUrl).isEqualTo(uploadUrl)
//
//        // Verify service was called with the tenant ID
//        coVerify { recordService.requestUpload(eq(tenantId), any()) }
//    }
//
//    @Test
//    fun `requestUpload should handle service errors`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val uploadRequestDto = UploadRequestDto(
//            batchId = UUID.randomUUID(),
//            fileName = "file.pdf",
//            type = RecordType.INVOICE,
//            createdBy = "user1"
//        )
//
//        val expectedCommand = UploadRequestCommand(
//            batchId = uploadRequestDto.batchId,
//            fileName = uploadRequestDto.fileName,
//            type = uploadRequestDto.type,
//            createdBy = uploadRequestDto.createdBy
//        )
//
//        // Mock service call to throw exception
//        coEvery {
//            recordService.requestUpload(tenantId, expectedCommand)
//        } throws RuntimeException("Service error")
//
//        // When/Then
//        try {
//            recordController.requestUpload(tenantId, uploadRequestDto)
//        } catch (e: RuntimeException) {
//            assertThat(e.message).isEqualTo("Service error")
//        }
//
//        // Verify service was called
//        coVerify(exactly = 1) {
//            recordService.requestUpload(tenantId, expectedCommand)
//        }
//    }
//
//    @Test
//    fun `requestUpload should pass tenant ID to service`() = runBlocking {
//        // Given
//        val tenantId = "custom-tenant-999"
//        val uploadRequestDto = UploadRequestDto(
//            batchId = UUID.randomUUID(),
//            fileName = "important-doc.pdf",
//            type = RecordType.INVOICE,
//            createdBy = "admin-user"
//        )
//
//        val expectedCommand = UploadRequestCommand(
//            batchId = uploadRequestDto.batchId,
//            fileName = uploadRequestDto.fileName,
//            type = uploadRequestDto.type,
//            createdBy = uploadRequestDto.createdBy
//        )
//
//        val serviceResult = UploadRequestResult(
//            recordId = UUID.randomUUID(),
//            uploadUrl = "https://example.com/upload-url"
//        )
//
//        // Mock service call
//        coEvery {
//            recordService.requestUpload(tenantId, expectedCommand)
//        } returns serviceResult
//
//        // When
//        recordController.requestUpload(tenantId, uploadRequestDto)
//
//        // Then
//        // Verify service was called with correct tenant ID
//        coVerify(exactly = 1) {
//            recordService.requestUpload(tenantId, expectedCommand)
//        }
//    }
//
//    @Test
//    fun `completeUpload should return OK when upload is completed successfully`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//
//        // Mock service call to do nothing (success case)
//        coEvery {
//            recordService.completeUpload(tenantId, recordId)
//        } just Runs
//
//        // When
//        val response = recordController.completeUpload(tenantId, recordId)
//
//        // Then
//        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
//
//        // Verify service was called with correct parameters
//        coVerify(exactly = 1) {
//            recordService.completeUpload(tenantId, recordId)
//        }
//    }
//
//    @Test
//    fun `completeUpload should use provided updatedBy value when available`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//
//        // Mock service call
//        coEvery {
//            recordService.completeUpload(tenantId, recordId)
//        } just Runs
//
//        // When
//        val response = recordController.completeUpload(tenantId, recordId)
//
//        // Then
//        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
//
//        // Verify service was called with correct updatedBy
//        coVerify(exactly = 1) {
//            recordService.completeUpload(tenantId, recordId)
//        }
//    }
//
//    @Test
//    fun `completeUpload should handle RecordNotFoundException`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//
//        // Mock service call to throw an exception
//        coEvery {
//            recordService.completeUpload(tenantId, recordId)
//        } throws RecordNotFoundException("Record not found")
//
//        // When/Then
//        assertThrows<RecordNotFoundException> {
//            runBlocking {
//                recordController.completeUpload(tenantId, recordId)
//            }
//        }
//
//        // Verify service was called
//        coVerify(exactly = 1) {
//            recordService.completeUpload(tenantId, recordId)
//        }
//    }
//
//    @Test
//    fun `completeUpload should handle StorageServiceException`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//
//        // Mock service call to throw an exception
//        coEvery {
//            recordService.completeUpload(tenantId, recordId)
//        } throws StorageServiceException("File not found in storage")
//
//        // When/Then
//        assertThrows<StorageServiceException> {
//            runBlocking {
//                recordController.completeUpload(tenantId, recordId)
//            }
//        }
//        // Verify service was called
//        coVerify(exactly = 1) {
//            recordService.completeUpload(tenantId, recordId)
//        }
//    }
//
//}