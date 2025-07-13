package com.muditsahni.controller.v1

import com.muditsahni.error.InvalidRequestException
import com.muditsahni.model.dto.request.CreateBatchRequestDto
import com.muditsahni.model.dto.response.CreateBatchResponseDto
import com.muditsahni.model.dto.response.toCreateBatchResponseDto
import com.muditsahni.service.v1.DefaultBatchService
import com.muditsahni.service.v1.DefaultTenantService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/tenants/{tenantName}/batches")
class BatchController(
    private val tenantService: DefaultTenantService,
    private val batchService: DefaultBatchService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Handles the creation of a new batch for a given tenant.
     * This endpoint allows users to create a batch by providing the necessary details
     * such as name, type, and optional tags.
     * @param tenantName The name of the tenant for which the batch is being created.
     * @param createBatchRequest The request body containing the details for creating the batch.
     * @return A [ResponseEntity] containing the created batch details or an error response if the creation fails.
     */
    @PostMapping("/create")
    suspend fun createBatch(
        @PathVariable tenantName: String,
        @RequestBody createBatchRequest: CreateBatchRequestDto
    ): ResponseEntity<CreateBatchResponseDto> {
        logger.info { "Received batch creation request for tenant $tenantName: ${createBatchRequest.name}" }

        // Validate tenant existence
        val tenant = tenantService.getTenantByName(tenantName)
            ?: throw InvalidRequestException("Tenant with name $tenantName does not exist")

        // Validate input
        if (createBatchRequest.name.isBlank()) {
            throw InvalidRequestException("Batch name cannot be empty")
        }

        try {
            // create record
            val createdBatch = batchService.createBatch(
                tenant,
                createBatchRequest.name,
                createBatchRequest.type
            )

            return ResponseEntity.ok(
                createdBatch.toCreateBatchResponseDto()
            )
        } catch (e: Exception) {
            logger.error(e) { "Error creating batch for tenant $tenantName: ${e.message}" }
            throw e
        }
    }

}