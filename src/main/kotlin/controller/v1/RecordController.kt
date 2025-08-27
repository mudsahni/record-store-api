package com.muditsahni.controller.v1

import com.muditsahni.config.TenantContext
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
import com.muditsahni.model.dto.request.CreateBatchRequestDto
import com.muditsahni.model.dto.request.CreateRecordRequestDto
import com.muditsahni.model.dto.response.CreateBatchResponseDto
import com.muditsahni.model.dto.response.CreateRecordResponseDto
import com.muditsahni.model.dto.response.toCreateBatchResponseDto
import com.muditsahni.model.entity.Role
import com.muditsahni.security.CoroutineSecurityUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/v1/records")
class RecordController(
    private val recordService: DefaultRecordService
) {

    private val logger = KotlinLogging.logger {}

    @Operation(summary = "Create a new Record")
    @ApiResponse(responseCode = "201", description = "Record created")
    @PostMapping("/create")
    suspend fun createRecord(
        @RequestBody createRecordRequest: CreateRecordRequestDto
    ): ResponseEntity<CreateBatchResponseDto> {
        logger.info { "Received record creation request : ${createRecordRequest.name}" }

        // Get authenticated user and tenant from JWT
        val currentUser = CoroutineSecurityUtils.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info {
            "User ${currentUser.email} creating record '${createRecordRequest.name}' in tenant ${currentTenant.name}"
        }

        return try {
            // Set tenant context and create batch
            withContext(TenantContext.setTenant(currentTenant)) {
                val createdBatch = recordService.createRecord(
                    currentTenant,
                    createRecordRequest.batchId,
                    createRecordRequest.name,
                    createRecordRequest.type,
                    createRecordRequest.fileType,
                    currentUser
                )

                ResponseEntity<CreateRecordResponseDto>(createdBatch.toCreateBatchResponseDto(), HttpStatus.CREATED)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating batch: ${e.message}" }
            throw e
        }
    }

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