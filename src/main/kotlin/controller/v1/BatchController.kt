package com.muditsahni.controller.v1

import com.muditsahni.config.TenantContext
import com.muditsahni.error.InvalidRequestException
import com.muditsahni.model.dto.request.CreateBatchRequestDto
import com.muditsahni.model.dto.response.CreateBatchResponseDto
import com.muditsahni.model.dto.response.toCreateBatchResponseDto
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.v1.DefaultBatchService
import com.muditsahni.service.v1.DefaultTenantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/batches")
class BatchController(
    private val tenantService: DefaultTenantService,
    private val batchService: DefaultBatchService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Handles the creation of a new batch for a given tenant.
     * This endpoint allows users to create a batch by providing the necessary details
     * such as name, type, and optional tags.
     * The user must be authenticated and belong to the specified tenant.
     * @param createBatchRequest The request body containing the details for creating the batch.
     * @return A [ResponseEntity] containing the created batch details or an error response if the creation fails.
     */
    @Operation(summary = "Create a new Batch")
    @ApiResponse(responseCode = "201", description = "Batch created")
    @PostMapping("/create")
    suspend fun createBatch(
        @RequestBody createBatchRequest: CreateBatchRequestDto
    ): ResponseEntity<CreateBatchResponseDto> {
        logger.info { "Received batch creation request : ${createBatchRequest.name}" }

        // Get authenticated user and tenant from JWT
        val currentUser = CoroutineSecurityUtils.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        logger.info {
            "User ${currentUser.email} creating batch '${createBatchRequest.name}' in tenant ${currentTenant.name}"
        }

        // Validate input
        if (createBatchRequest.name.isBlank()) {
            throw InvalidRequestException("Batch name cannot be empty")
        }

        return try {
            // Set tenant context and create batch
            withContext(TenantContext.setTenant(currentTenant)) {
                val createdBatch = batchService.createBatch(
                    currentTenant,
                    createBatchRequest.name,
                    createBatchRequest.type,
                    createBatchRequest.tags,
                    currentUser
                )

                ResponseEntity<CreateBatchResponseDto>(createdBatch.toCreateBatchResponseDto(), HttpStatus.CREATED)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating batch: ${e.message}" }
            throw e
        }
    }

    /**
     * Handles the retrieval of all batches for the authenticated user's tenant.
     * This endpoint allows users with ADMIN or SUPER_ADMIN roles to fetch all batches
     * associated with their tenant.
     * @return A [ResponseEntity] containing a list of batch details or an error response if the retrieval fails.
     */
    @Operation(summary = "Get all Batches")
    @ApiResponse(responseCode = "200", description = "Batches fetched")
    @GetMapping
    suspend fun getAllBatches(): ResponseEntity<List<CreateBatchResponseDto>> {
        logger.info { "Received request to fetch all batches" }

        // Get authenticated user and tenant from JWT
        val currentUser = CoroutineSecurityUtils.getCurrentUser()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        if (currentUser.roles.none { it.name == "ADMIN" || it.name == "SUPER_ADMIN" }) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info {
            "User ${currentUser.email} fetching all batches in tenant ${currentTenant.name}"
        }

        return try {
            // Set tenant context and fetch batches
            withContext(TenantContext.setTenant(currentTenant)) {
                val batches = batchService.getAllBatches()
                    .toList()
                    .map { it.toCreateBatchResponseDto() }

                ResponseEntity.ok(batches)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error fetching batches: ${e.message}" }
            throw e
        }
    }
}