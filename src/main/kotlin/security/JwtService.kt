package com.muditsahni.security

import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {
    @Value("\${jwt.secret:something}")
    private lateinit var secret: String

    @Value("\${jwt.expiration:86400000}") // 24 hours
    private val expiration: Long = 86400000

    @Value("\${jwt.refresh.expiration:604800000}") // 7 days
    private val refreshExpiration: Long = 604800000

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    }

    fun generateToken(user: User, tenant: Tenant): String {
        return buildToken(user, tenant, expiration)
    }

    fun generateRefreshToken(user: User, tenant: Tenant): String {
        return buildToken(user, tenant, refreshExpiration)
    }

    private fun buildToken(user: User, tenant: Tenant, expirationTime: Long): String {
        return Jwts.builder()
            .setSubject(user.id.toString())
            .claim("email", user.email)
            .claim("tenantId", tenant.id.toString())
            .claim("tenantName", tenant.name)
            .claim("roles", user.roles)
            .claim("type", if (expirationTime == refreshExpiration) "refresh" else "access")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + expirationTime))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun extractUserId(token: String): UUID {
        return UUID.fromString(extractClaim(token, Claims::getSubject))
    }

    fun extractTenantId(token: String): UUID {
        return UUID.fromString(extractClaim(token) { it["tenantId"] as String })
    }

    fun extractTenantName(token: String): String {
        return extractClaim(token) { it["tenantName"] as String }
    }

    fun extractRoles(token: String): List<String> {
        return extractClaim(token) {
            @Suppress("UNCHECKED_CAST")
            it["roles"] as List<String>
        }
    }

    fun extractEmail(token: String): String {
        return extractClaim(token) { it["email"] as String }
    }

    fun isRefreshToken(token: String): Boolean {
        return try {
            extractClaim(token) { it["type"] as? String } == "refresh"
        } catch (e: Exception) {
            false
        }
    }

    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            extractAllClaims(token)
            !isTokenExpired(token)
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractClaim(token, Claims::getExpiration).before(Date())
    }

    fun getExpirationDate(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }
}