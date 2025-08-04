package com.muditsahni.service.v1

import com.muditsahni.constant.General
import com.muditsahni.error.DomainAlreadyExistsException
import com.muditsahni.error.TenantAlreadyExistsException
import com.muditsahni.model.entity.Domain
import com.muditsahni.model.entity.Tenant
import com.muditsahni.repository.global.DomainRepository
import com.muditsahni.repository.global.TenantRepository
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.TenantService
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DefaultTenantService(
    override val tenantRepository: TenantRepository,
    override val domainRepository: DomainRepository
) : TenantService {

    /**
     * Creates a new tenant with the specified name and type.
     * @param name The name of the tenant.
     * @param type The type of the tenant.
     * @param domains The set of domains associated with the tenant.
     * @return The created [Tenant] object.
     */
    override suspend fun createTenant(
        name: String,
        type: String,
        domains: Set<String>
    ): Tenant {
        if (tenantRepository.findByName(name) != null) {
            throw TenantAlreadyExistsException(name)
        }

        // Validate domains are not already taken
        domains.forEach { domain ->
            if (domainRepository.existsByNameAndDeletedFalse(domain)) {
                throw DomainAlreadyExistsException(domain)
            }
        }

        val createdBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        // Create tenant first
        val tenant = tenantRepository.save(
            Tenant(name = name, type = type, domains = domains, createdBy = createdBy)
        )

        // Create domain records
        domains.forEach { domainName ->
            domainRepository.save(
                Domain(
                    name = domainName,
                    tenantName = tenant.name,
                    createdAt = Instant.now(),
                    createdBy = createdBy
                )
            )
        }

        return tenant
    }

    /**
     * Retrieves a tenant by its name.
     * @param name The name of the tenant to retrieve.
     * @return The [Tenant] object if found, or null if not found or deleted.
     */
    override suspend fun getTenantByName(name: String): Tenant? {
        val tenant = tenantRepository
            .findByName(name)

        if (tenant?.deleted == true) {
            return null
        }

        return tenant
    }

    /**
     * Deletes a tenant by marking it as deleted.
     * This method updates the tenant's `deleted` flag to true
     * instead of removing it from the database.
     * @param name The name of the tenant to delete.
     * @return True if the tenant was successfully marked as deleted, false if the tenant does not exist.
     */
    override suspend fun deleteTenant(
        name: String,
    ): Boolean {
        val tenant = getTenantByName(name) ?: return false

        val updatedBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        tenant.deleted = true
        tenant.updatedAt = Instant.now()
        tenant.updatedBy = updatedBy
        tenantRepository.save(tenant)
        return true
    }

}