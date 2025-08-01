package com.muditsahni.security

import com.muditsahni.repository.TenantRepository
import com.muditsahni.repository.UserRepository
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtAuthenticationWebFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val authHeader = request.headers.getFirst("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange)
        }

        val token = authHeader.substring(7)

        return mono {
            try {
                if (!jwtService.isTokenValid(token)) {
                    throw RuntimeException("Invalid or expired token")
                }

                val userId = jwtService.extractUserId(token)
                val tenantId = jwtService.extractTenantId(token)
                val tenantName = jwtService.extractTenantName(token)

                // Verify user exists and belongs to the tenant
                val user = userRepository.findById(userId)
                    ?: throw RuntimeException("User not found")

                if (user.tenantName != tenantName) {
                    throw RuntimeException("Tenant mismatch")
                }

                // Verify tenant exists
                val tenant = tenantRepository.findByName(tenantName)
                    ?: throw RuntimeException("Tenant not found")

                if (tenant.name != tenantName || tenant.deleted) {
                    throw RuntimeException("Tenant not found or inactive")
                }

                // Create principal and set authentication
                val principal = TenantUserPrincipal(
                    userId = userId,
                    email = user.email,
                    tenantId = tenantId,
                    tenantName = tenantName,
                    roles = user.roles,
                    user = user,
                    tenant = tenant
                )

                val authentication = TenantAuthenticationToken(principal, principal.authorities)
                authentication

            } catch (e: Exception) {
                throw e
            }
        }.flatMap { authentication ->
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        }.onErrorResume { e ->
            createErrorResponse(exchange, "Authentication failed: ${e.message}")
        }
    }

    private fun createErrorResponse(exchange: ServerWebExchange, message: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")

        val buffer = response.bufferFactory().wrap("""{"error": "$message"}""".toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}
