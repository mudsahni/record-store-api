package com.muditsahni.controller.v1

import com.muditsahni.model.dto.request.CreateTenantRequestDto
import com.muditsahni.model.dto.response.TenantResponseDto
import com.muditsahni.model.dto.response.toTenantResponseDto
import com.muditsahni.model.entity.Role
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.v1.DefaultTenantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Tenants", description = "Create & lookup tenants")
@Validated
@RestController
@RequestMapping("/api/v1/tenants")
class TenantController(
    private val tenantService: DefaultTenantService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Handles the creation of a new tenant.
     * This endpoint allows users to create a tenant by providing the necessary details
     * such as name and type.
     * @param createTenantRequestDto The request body containing the details for creating the tenant.
     * @return A [ResponseEntity] containing the created tenant details or an error response if the creation fails.
     */
    @Operation(
        summary = "Create a new tenant",
        description = "Takes name & type, returns the created tenant DTO"
    )
    @ApiResponse(responseCode = "200", description = "Tenant created")
    @PostMapping()
    suspend fun createTenant(
        @Valid @RequestBody createTenantRequestDto: CreateTenantRequestDto
    ): ResponseEntity<TenantResponseDto> {

        if (!CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info {
            "Received tenant creation request: ${createTenantRequestDto.name}, type: ${createTenantRequestDto.type}"
        }

        try {
            // create record
            val createdTenant = tenantService.createTenant(
                createTenantRequestDto.name,
                createTenantRequestDto.type,
                createTenantRequestDto.domains
            )

            return ResponseEntity.ok(
                createdTenant.toTenantResponseDto()
            )
        } catch (e: Exception) {
            logger.error {
                "Error processing tenant creation request: ${e.message}"
            }
            throw e
        }
    }

    /**
     * Retrieves a tenant by its name.
     * This endpoint allows users to fetch the details of a tenant
     * by providing its name.
     * @param tenantName The name of the tenant to retrieve.
     * @return A [ResponseEntity] containing the tenant details or an error response if the tenant does not exist.
     */
    @Operation(
        summary = "Get tenant by name",
        description = "Fetches tenant details by name"
    )
    @ApiResponse(responseCode = "200", description = "Tenant found")
    @GetMapping("/{tenantName}")
    suspend fun getTenantByName(
        @PathVariable tenantName: String
    ): ResponseEntity<TenantResponseDto> {

        val currentTenant = CoroutineSecurityUtils.getCurrentTenant()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        // Users can only see their own tenant, unless they're admin
        val hasAdminAccess = CoroutineSecurityUtils.hasAnyRole(Role.ADMIN, Role.SUPER_ADMIN)
        if (!hasAdminAccess && currentTenant.name != tenantName) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info { "Received request to get tenant by name: $tenantName" }

        try {
            val tenant = tenantService.getTenantByName(tenantName)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

            return ResponseEntity.ok(
                tenant.toTenantResponseDto()
            )
        } catch (e: Exception) {
            logger.error { "Error retrieving tenant: ${e.message}" }
            throw e
        }
    }

    /**
     * Deletes a tenant by its name.
     * This endpoint allows users to delete a tenant by marking it as deleted.
     * @param tenantName The name of the tenant to delete.
     * @return A [ResponseEntity] indicating the success or failure of the deletion operation.
     */
    @Operation(
        summary = "Delete tenant by name",
        description = "Deletes a tenant by marking it as deleted"
    )
    @ApiResponse(responseCode = "200", description = "Tenant deleted successfully")
    @DeleteMapping("/{tenantName}")
    suspend fun deleteTenantByName(
        @PathVariable tenantName: String
    ): ResponseEntity<String> {

        if (!CoroutineSecurityUtils.hasRole(Role.SUPER_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        logger.info { "Received request to delete tenant by name: $tenantName" }

        return if (tenantService.deleteTenant(tenantName)) {
            ResponseEntity.ok("Tenant $tenantName deleted successfully")
        } else {
            ResponseEntity.notFound().build()
        }
    }
}