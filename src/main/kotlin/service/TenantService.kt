package com.muditsahni.service

import com.muditsahni.model.entity.Tenant
import com.muditsahni.repository.TenantRepository

interface TenantService {
    /**
     * Repository for managing tenant entities.
     */
    val tenantRepository: TenantRepository

    /**
     * Creates a new tenant with the specified name and type.
     * @param name The name of the tenant.
     * @param type The type of the tenant.
     * @return The created [Tenant] object.
     */
    suspend fun createTenant(
        name: String,
        type: String,
    ): Tenant

    /**
     * Retrieves a tenant by its name.
     * @param name The name of the tenant to retrieve.
     * @return The [Tenant] object if found, or null if not found.
     */
    suspend fun getTenantByName(
        name: String,
    ): Tenant?


    /**
     * Deletes a tenant.
     * @param name The name of the tenant to delete.
     * @return True if the tenant was successfully marked as deleted, false if the tenant does not exist.
     */
    suspend fun deleteTenant(
        name: String,
    ): Boolean
}