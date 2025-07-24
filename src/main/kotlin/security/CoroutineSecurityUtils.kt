package com.muditsahni.security

import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import java.util.*

object CoroutineSecurityUtils {
    suspend fun getCurrentPrincipal(): TenantUserPrincipal? {
        return try {
            ReactiveSecurityContextHolder.getContext()
                .map { it.authentication }
                .cast(TenantAuthenticationToken::class.java)
                .map { it.principal }
                .awaitSingleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentUser(): User? = getCurrentPrincipal()?.user
    suspend fun getCurrentTenant(): Tenant? = getCurrentPrincipal()?.tenant
    suspend fun getCurrentTenantId(): UUID? = getCurrentPrincipal()?.tenantId
    suspend fun getCurrentUserId(): UUID? = getCurrentPrincipal()?.userId
    suspend fun getCurrentUserEmail(): String? = getCurrentPrincipal()?.email
    suspend fun getCurrentUserRoles(): List<Role> = getCurrentPrincipal()?.roles ?: emptyList()

    suspend fun hasRole(role: Role): Boolean {
        return getCurrentUserRoles().contains(role)
    }

    suspend fun hasAnyRole(vararg roles: Role): Boolean {
        val userRoles = getCurrentUserRoles()
        return roles.any { userRoles.contains(it) }
    }
}