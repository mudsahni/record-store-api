//package com.muditsahni.service.v1
//
//import io.mockk.*
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.assertj.core.api.Assertions.assertThat
//import com.muditsahni.error.RecordNotFoundException
//import com.muditsahni.error.StorageServiceException
//import java.util.UUID
//import com.muditsahni.models.entity.Record
//import com.muditsahni.models.entity.RecordStatus
//import com.muditsahni.models.entity.RecordType
//import com.muditsahni.repository.RecordRepository
//import com.muditsahni.service.AzureBlobStorageService
//import com.muditsahni.service.command.UploadRequestCommand
//import org.junit.jupiter.api.assertThrows
//import java.time.Instant
//
//
//class DefaultRecordServiceTest {
//
//    private lateinit var recordRepository: RecordRepository
//    private lateinit var storageService: AzureBlobStorageService
//    private lateinit var recordService: DefaultRecordService
//
//    @BeforeEach
//    fun setup() {
//        recordRepository = mockk()
//        storageService = mockk()
//        recordService = DefaultRecordService(recordRepository, storageService)
//    }
//
//    @Test
//    fun `requestUpload should save record and generate upload URL`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//        val batchId = UUID.randomUUID()
//        val fileName = "test-document.pdf"
//        val recordType = RecordType.INVOICE
//        val createdBy = "user1"
//
//        val command = UploadRequestCommand(
//            batchId = batchId,
//            fileName = fileName,
//            type = recordType,
//            createdBy = createdBy
//        )
//
//        val expectedRecord = Record(
//            batchId = batchId,
//            fileName = fileName,
//            type = recordType,
//            createdBy = createdBy,
//            status = RecordStatus.CREATED
//        )
//
//        val savedRecord = expectedRecord.copy(id = recordId)
//
//        val expectedUploadUrl = "https://example.blob.core.windows.net/container/records/${recordId}/test-document.pdf?sig=signature"
//
//        // Mock repository save
//        coEvery { recordRepository.save(any()) } returns savedRecord
//
//        // Mock storage service
//        every { storageService.generateUploadUrl(recordId, fileName) } returns expectedUploadUrl
//
//        // When
//        val result = recordService.requestUpload(tenantId, command)
//
//        // Then
//        assertThat(result.recordId).isEqualTo(recordId)
//        assertThat(result.uploadUrl).isEqualTo(expectedUploadUrl)
//
//        // Verify record was saved with correct properties
//        coVerify {
//            recordRepository.save(match { record ->
//                record.batchId == batchId &&
//                        record.fileName == fileName &&
//                        record.type == recordType &&
//                        record.createdBy == createdBy &&
//                        record.status == RecordStatus.CREATED
//            })
//        }
//
//        // Verify upload URL was generated
//        verify { storageService.generateUploadUrl(recordId, fileName) }
//    }
//
//    @Test
//    fun `requestUpload should handle repository exceptions`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val command = UploadRequestCommand(
//            batchId = UUID.randomUUID(),
//            fileName = "test.pdf",
//            type = RecordType.INVOICE,
//            createdBy = "user1"
//        )
//
//        // Mock repository to throw exception
//        coEvery { recordRepository.save(any()) } throws RuntimeException("Database error")
//
//        // When/Then
//        try {
//            recordService.requestUpload(tenantId, command)
//        } catch (e: RuntimeException) {
//            assertThat(e.message).isEqualTo("Database error")
//        }
//
//        // Verify repository was called
//        coVerify { recordRepository.save(any()) }
//
//        // Verify storage service was NOT called
//        verify(exactly = 0) { storageService.generateUploadUrl(any(), any()) }
//    }
//
//    @Test
//    fun `requestUpload should handle storage service exceptions`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//        val fileName = "test.pdf"
//
//        val command = UploadRequestCommand(
//            batchId = UUID.randomUUID(),
//            fileName = fileName,
//            type = RecordType.INVOICE,
//            createdBy = "user1"
//        )
//
//        val savedRecord = Record(
//            id = recordId,
//            batchId = command.batchId,
//            fileName = command.fileName,
//            type = command.type,
//            createdBy = command.createdBy,
//            status = RecordStatus.CREATED
//        )
//
//        // Mock repository save
//        coEvery { recordRepository.save(any()) } returns savedRecord
//
//        // Mock storage service to throw exception
//        every { storageService.generateUploadUrl(recordId, fileName) } throws
//                RuntimeException("Storage service error")
//
//        // When/Then
//        try {
//            recordService.requestUpload(tenantId, command)
//        } catch (e: RuntimeException) {
//            assertThat(e.message).isEqualTo("Storage service error")
//        }
//
//        // Verify repository was called
//        coVerify { recordRepository.save(any()) }
//
//        // Verify storage service was called
//        verify { storageService.generateUploadUrl(recordId, fileName) }
//    }
//
//    @Test
//    fun `completeUpload should update record status when record and blob exist`() {
//        runBlocking {
//            // Given
//            val tenantId = "tenant-123"
//            val recordId = UUID.randomUUID()
//            val fileName = "test-document.pdf"
//
//            // Create a record with initial null updatedAt
//            val initialRecord = Record(
//                id = recordId,
//                batchId = UUID.randomUUID(),
//                fileName = fileName,
//                type = RecordType.INVOICE,
//                status = RecordStatus.CREATED,
//                createdBy = "user1",
//                createdAt = Instant.now().minusSeconds(60),
//                updatedAt = null,  // Initial updatedAt is null
//                updatedBy = null   // Initial updatedBy is null
//            )
//
//            // Capture the saved record
//            val recordSlot = slot<Record>()
//
//            // Mock repository and storage service
//            coEvery { recordRepository.findById(recordId) } returns initialRecord
//            every { storageService.blobExists(recordId, fileName) } returns true
//            coEvery { recordRepository.save(capture(recordSlot)) } returns initialRecord.copy(
//                status = RecordStatus.UPLOADED,
//                updatedAt = Instant.now(),
//                updatedBy = "system"
//            )
//
//            // When
//            recordService.completeUpload(tenantId, recordId)
//
//            // Then
//            coVerify(exactly = 1) { recordRepository.findById(recordId) }
//            verify(exactly = 1) { storageService.blobExists(recordId, fileName) }
//            coVerify(exactly = 1) { recordRepository.save(any()) }
//
//            // Assert on the captured record
//            with(recordSlot.captured) {
//                assertThat(id).isEqualTo(recordId)
//                assertThat(status).isEqualTo(RecordStatus.UPLOADED)
//
//                // Check that updatedAt is no longer null
//                assertThat(updatedAt).isNotNull()
//
//                // Check that updatedBy is set to "system"
//                assertThat(updatedBy).isEqualTo("system")
//
//                // If initialRecord.updatedAt was null, we can't compare "after"
//                // But we can verify it's now a recent timestamp
//                val now = Instant.now()
//                val fiveMinutesAgo = now.minusSeconds(300)
//                assertThat(updatedAt).isBetween(fiveMinutesAgo, now)
//            }
//        }
//    }
//    @Test
//    fun `completeUpload should throw RecordNotFoundException when record does not exist`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//
//        // Mock repository to return null (record not found)
//        coEvery { recordRepository.findById(recordId) } returns null
//
//        // When/Then
//        val exception = assertThrows<RecordNotFoundException> {
//            runBlocking {
//                recordService.completeUpload(tenantId, recordId)
//            }
//        }
//
//        assertThat(exception.message).contains("Record not found")
//
//        // Verify repository was called to find the record
//        coVerify(exactly = 1) { recordRepository.findById(recordId) }
//
//        // Verify storage service was never called
//        verify(exactly = 0) { storageService.blobExists(any(), any()) }
//
//        // Verify save was never called
//        coVerify(exactly = 0) { recordRepository.save(any()) }
//    }
//
//    @Test
//    fun `completeUpload should throw StorageServiceException when blob does not exist`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//        val fileName = "test-document.pdf"
//
//        val record = Record(
//            id = recordId,
//            batchId = UUID.randomUUID(),
//            fileName = fileName,
//            type = RecordType.INVOICE,
//            status = RecordStatus.CREATED,
//            createdBy = "user1",
//            updatedBy = "user1",
//            createdAt = Instant.now().minusSeconds(60),
//            updatedAt = Instant.now().minusSeconds(60)
//        )
//
//        // Mock repository and storage service
//        coEvery { recordRepository.findById(recordId) } returns record
//        every { storageService.blobExists(recordId, fileName) } returns false
//
//        // When/Then
//        val exception = assertThrows<StorageServiceException> {
//            runBlocking {
//                recordService.completeUpload(tenantId, recordId)
//            }
//        }
//
//        assertThat(exception.message).contains("File not found in storage")
//
//        // Verify repository and storage service were called
//        coVerify(exactly = 1) { recordRepository.findById(recordId) }
//        verify(exactly = 1) { storageService.blobExists(recordId, fileName) }
//
//        // Verify save was never called
//        coVerify(exactly = 0) { recordRepository.save(any()) }
//    }
//
//    @Test
//    fun `completeUpload should handle repository exceptions`() = runBlocking {
//        // Given
//        val tenantId = "tenant-123"
//        val recordId = UUID.randomUUID()
//        val fileName = "test-document.pdf"
//
//        val record = Record(
//            id = recordId,
//            batchId = UUID.randomUUID(),
//            fileName = fileName,
//            type = RecordType.INVOICE,
//            status = RecordStatus.CREATED,
//            createdBy = "user1",
//            updatedBy = "user1",
//            createdAt = Instant.now().minusSeconds(60),
//            updatedAt = Instant.now().minusSeconds(60)
//        )
//
//        // Mock repository and storage service
//        coEvery { recordRepository.findById(recordId) } returns record
//        every { storageService.blobExists(recordId, fileName) } returns true
//        coEvery { recordRepository.save(any()) } throws RuntimeException("Database error")
//
//        // When/Then
//        val exception = assertThrows<RuntimeException> {
//            runBlocking {
//                recordService.completeUpload(tenantId, recordId)
//            }
//        }
//
//        assertThat(exception.message).contains("Database error")
//
//        // Verify correct methods were called
//        coVerify(exactly = 1) { recordRepository.findById(recordId) }
//        verify(exactly = 1) { storageService.blobExists(recordId, fileName) }
//        coVerify(exactly = 1) { recordRepository.save(any()) }
//    }
//}