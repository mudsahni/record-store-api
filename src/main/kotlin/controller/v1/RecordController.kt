package com.muditsahni.controller.v1

import mu.KotlinLogging
import com.muditsahni.models.dto.request.UploadRequestDto
import com.muditsahni.models.dto.request.toUploadRequestCommand
import com.muditsahni.model.dto.response.UploadRequestResponseDto
import com.muditsahni.model.dto.response.toUploadRequestResponseDto
import com.muditsahni.service.v1.DefaultRecordService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

import com.muditsahni.error.InvalidRequestException
import com.muditsahni.model.entity.Role
import com.muditsahni.security.CoroutineSecurityUtils
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/v1/tenants/{tenantName}/records")
class RecordController(
    private val recordService: DefaultRecordService
) {

    private val logger = KotlinLogging.logger {}

    // Upload workflows
    @PostMapping("/upload/request")
    suspend fun requestUpload(
        @PathVariable tenantName: String,
        @RequestBody uploadRequest: UploadRequestDto
    ): ResponseEntity<UploadRequestResponseDto> {
        val userTenant = CoroutineSecurityUtils.getCurrentTenant()

        if (userTenant?.name != tenantName) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (!CoroutineSecurityUtils.hasAnyRole(Role.USER, Role.ADMIN, Role.SUPER_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info { "Received upload request for tenant $tenantName: ${uploadRequest.fileName}" }
        // Validate input
        if (uploadRequest.fileName.isBlank()) {
            throw InvalidRequestException("File name cannot be empty")
        }

        try {
            // create record
            val uploadRequestResult = recordService.requestUpload(
                tenantName,
                uploadRequest.toUploadRequestCommand()
            )
            logger.info { "Upload URL generated for record ${uploadRequestResult.recordId}" }
            return ResponseEntity.ok(
                uploadRequestResult.toUploadRequestResponseDto()
            )
        } catch (e: Exception) {
            logger.error(e) { "Error processing upload request: ${e.message}" }
            throw e
        }
    }

    @PutMapping("/{recordId}/upload/complete")
    suspend fun completeUpload(
        @PathVariable tenantName: String,
        @PathVariable recordId: UUID
    ): ResponseEntity<Void>  {
        val userTenant = CoroutineSecurityUtils.getCurrentTenant()

        if (userTenant?.name != tenantName) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        if (!CoroutineSecurityUtils.hasAnyRole(Role.USER, Role.ADMIN, Role.SUPER_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info { "Processing upload completion for tenant $tenantName, record $recordId" }
        try {
            recordService.completeUpload(tenantName, recordId)
            logger.info { "Upload completed successfully for record $recordId" }
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            logger.error(e) { "Error completing upload: ${e.message}" }
            throw e
        }
    }
//
//    @PostMapping("/{recordId}/validate/request")
//    fun requestValidation(@PathVariable recordId: UUID): ResponseEntity<ValidationRequestResponseDto> {
//        val response = recordService.createValidationRequest(recordId)
//        return ResponseEntity.ok(response)
//    }
//
//    @PutMapping("/{recordId}/validate/complete")
//    fun completeValidation(
//        @PathVariable recordId: UUID,
//        @RequestBody validationResult: ValidationResultDto
//    ): ResponseEntity<Record> {
//        val updatedRecord = recordService.completeValidation(recordId, validationResult)
//        return ResponseEntity.ok(updatedRecord)
//    }
//
//    // Standard CRUD operations
//    @PutMapping("/{recordId}/update")
//    fun updateRecord(
//        @PathVariable recordId: UUID,
//        @RequestBody recordUpdateDto: RecordUpdateDto
//    ): ResponseEntity<Record> {
//        val updatedRecord = recordService.updateRecord(recordId, recordUpdateDto)
//        return ResponseEntity.ok(updatedRecord)
//    }
//
//    @GetMapping("/{recordId}/get")
//    fun getRecord(@PathVariable recordId: UUID): ResponseEntity<Record> {
//        val record = recordService.getRecord(recordId)
//        return ResponseEntity.ok(record)
//    }
//
//    @DeleteMapping("/{recordId}/delete")
//    fun deleteRecord(@PathVariable recordId: UUID): ResponseEntity<Void> {
//        recordService.deleteRecord(recordId)
//        return ResponseEntity(HttpStatus.NO_CONTENT)
//    }
//
//    // Additional recommended endpoints
//
//    // Get all records (with pagination and filtering)
//    @GetMapping
//    fun getAllRecords(
//        @RequestParam(required = false) status: RecordStatus?,
//        @RequestParam(required = false) documentType: DocumentType?,
//        @RequestParam(required = false) batchId: UUID?,
//        @RequestParam(defaultValue = "0") page: Int,
//        @RequestParam(defaultValue = "20") size: Int
//    ): ResponseEntity<Page<Record>> {
//        val records = recordService.findRecords(status, documentType, batchId, page, size)
//        return ResponseEntity.ok(records)
//    }
//
//    // Batch operations
//    @PostMapping("/batch/{batchId}/status")
//    fun updateBatchStatus(
//        @PathVariable batchId: UUID,
//        @RequestParam status: RecordStatus
//    ): ResponseEntity<BatchUpdateResultDto> {
//        val result = recordService.updateBatchStatus(batchId, status)
//        return ResponseEntity.ok(result)
//    }
//
//    // Get records by batch
//    @GetMapping("/batch/{batchId}")
//    fun getRecordsByBatch(
//        @PathVariable batchId: UUID,
//        @RequestParam(defaultValue = "0") page: Int,
//        @RequestParam(defaultValue = "20") size: Int
//    ): ResponseEntity<Page<Record>> {
//        val records = recordService.findRecordsByBatch(batchId, page, size)
//        return ResponseEntity.ok(records)
//    }
}