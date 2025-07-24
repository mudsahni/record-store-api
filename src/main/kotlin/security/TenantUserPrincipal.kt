package com.muditsahni.security

import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class TenantUserPrincipal(
    val userId: UUID,
    val email: String,
    val tenantId: UUID,
    val tenantName: String,
    val roles: List<Role>,
    val user: User,
    val tenant: Tenant
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
    }

    override fun getPassword(): String = ""
    override fun getUsername(): String = email
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}

class TenantAuthenticationToken(
    private val principal: TenantUserPrincipal,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any = ""
    override fun getPrincipal(): TenantUserPrincipal = principal
}