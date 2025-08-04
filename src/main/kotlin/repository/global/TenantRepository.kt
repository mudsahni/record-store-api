package com.muditsahni.repository.global

import com.muditsahni.model.entity.Tenant
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TenantRepository : CoroutineCrudRepository<Tenant, String> {

    /**
     * Finds a tenant by its name.
     * @param name The name of the tenant to search for.
     * @return The [Tenant] object if found, or null if not found.
     */
    suspend fun findByName(name: String): Tenant?
}