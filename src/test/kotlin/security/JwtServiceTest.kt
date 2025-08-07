package security

import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.security.JwtService
import io.jsonwebtoken.MalformedJwtException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.*
import org.junit.jupiter.api.Assertions.*
import kotlin.math.abs

class JwtServiceTest {

    private lateinit var jwtService: JwtService
    private lateinit var testUser: User
    private lateinit var testTenant: Tenant

    @BeforeEach
    fun setUp() {
        jwtService = JwtService()

        // Set test values using reflection (simulating @Value injection)
        ReflectionTestUtils.setField(jwtService, "secret", "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZy1mb3ItaHMyNTY=")
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L)
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L)
        ReflectionTestUtils.setField(jwtService, "verificationExpiration", 86400000L)

        // Create test data
        testUser = User(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            phoneNumber = "+1234567890",
            passwordHash = "hashedPassword",
            tenantName = "test-tenant",
            roles = listOf(Role.USER),
            createdBy = "system",
            status = UserStatus.ACTIVE,
            createdAt = Instant.now(),
            emailVerified = true
        )

        testTenant = Tenant(
            id = UUID.randomUUID(),
            name = "test-tenant",
            type = "organization",
            createdBy = "admin",
            createdAt = Instant.now(),
            domains = setOf("example.com"),
        )
    }

    @Test
    fun `generateToken should create valid access token`() {
        // When
        val token = jwtService.generateToken(testUser, testTenant)

        // Then
        assertNotNull(token)
        assertFalse(token.isEmpty())
        assertTrue(jwtService.isTokenValid(token))
        assertFalse(jwtService.isRefreshToken(token))
        assertFalse(jwtService.isVerificationToken(token))
    }

    @Test
    fun `generateRefreshToken should create valid refresh token`() {
        // When
        val refreshToken = jwtService.generateRefreshToken(testUser, testTenant)

        // Then
        assertNotNull(refreshToken)
        assertFalse(refreshToken.isEmpty())
        assertTrue(jwtService.isTokenValid(refreshToken))
        assertTrue(jwtService.isRefreshToken(refreshToken))
        assertFalse(jwtService.isVerificationToken(refreshToken))
    }

    @Test
    fun `generateVerificationToken should create valid verification token`() {
        // When
        val verificationToken = jwtService.generateVerificationToken(testUser.email, testTenant.name)

        // Then
        assertNotNull(verificationToken)
        assertFalse(verificationToken.isEmpty())
        assertTrue(jwtService.isTokenValid(verificationToken))
        assertTrue(jwtService.isVerificationToken(verificationToken))
        assertFalse(jwtService.isRefreshToken(verificationToken))
    }

    @Test
    fun `extractUserId should return correct user ID from access token`() {
        // Given
        val token = jwtService.generateToken(testUser, testTenant)

        // When
        val extractedUserId = jwtService.extractUserId(token)

        // Then
        assertEquals(testUser.id, extractedUserId)
    }

    @Test
    fun `extractTenantId should return correct tenant ID from access token`() {
        // Given
        val token = jwtService.generateToken(testUser, testTenant)

        // When
        val extractedTenantId = jwtService.extractTenantId(token)

        // Then
        assertEquals(testTenant.id, extractedTenantId)
    }

    @Test
    fun `extractTenantName should return correct tenant name from access token`() {
        // Given
        val token = jwtService.generateToken(testUser, testTenant)

        // When
        val extractedTenantName = jwtService.extractTenantName(token)

        // Then
        assertEquals(testTenant.name, extractedTenantName)
    }

    @Test
    fun `extractEmail should return correct email from access token`() {
        // Given
        val token = jwtService.generateToken(testUser, testTenant)

        // When
        val extractedEmail = jwtService.extractEmail(token)

        // Then
        assertEquals(testUser.email, extractedEmail)
    }

    @Test
    fun `extractRoles should return correct roles from access token`() {
        // Given
        val token = jwtService.generateToken(testUser, testTenant)

        // When
        val extractedRoles = jwtService.extractRoles(token)

        // Then
        assertEquals(testUser.roles.map { it.name }, extractedRoles)
    }

    @Test
    fun `parseVerificationToken should return correct data for valid verification token`() {
        // Given
        val verificationToken = jwtService.generateVerificationToken(testUser.email, testTenant.name)

        // When
        val tokenData = jwtService.parseVerificationToken(verificationToken)

        // Then
        assertNotNull(tokenData)
        assertEquals(testUser.email, tokenData?.email)
        assertEquals(testTenant.name, tokenData?.tenantName)
    }

    @Test
    fun `parseVerificationToken should return null for invalid token`() {
        // Given
        val invalidToken = "invalid.token.here"

        // When
        val tokenData = jwtService.parseVerificationToken(invalidToken)

        // Then
        assertNull(tokenData)
    }

    @Test
    fun `parseVerificationToken should return null for non-verification token`() {
        // Given
        val accessToken = jwtService.generateToken(testUser, testTenant)

        // When
        val tokenData = jwtService.parseVerificationToken(accessToken)

        // Then
        assertNull(tokenData)
    }

    @Test
    fun `isTokenValid should return true for valid token`() {
        // Given
        val token = jwtService.generateToken(testUser, testTenant)

        // When & Then
        assertTrue(jwtService.isTokenValid(token))
    }

    @Test
    fun `isTokenValid should return false for invalid token`() {
        // Given
        val invalidToken = "invalid.token.here"

        // When & Then
        assertFalse(jwtService.isTokenValid(invalidToken))
    }

    @Test
    fun `isTokenValid should return false for malformed token`() {
        // Given
        val malformedToken = "malformed"

        // When & Then
        assertFalse(jwtService.isTokenValid(malformedToken))
    }

    @Test
    fun `isTokenValid should return false for expired token`() {
        // Given - create service with very short expiration
        val shortExpirationService = JwtService()
        ReflectionTestUtils.setField(shortExpirationService, "secret", "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZy1mb3ItaHMyNTY=")
        ReflectionTestUtils.setField(shortExpirationService, "expiration", 1L) // 1ms
        ReflectionTestUtils.setField(shortExpirationService, "refreshExpiration", 1L)
        ReflectionTestUtils.setField(shortExpirationService, "verificationExpiration", 1L)

        val token = shortExpirationService.generateToken(testUser, testTenant)

        // Wait for token to expire
        Thread.sleep(10)

        // When & Then
        assertFalse(shortExpirationService.isTokenValid(token))
    }

    @Test
    fun `isRefreshToken should return true for refresh token`() {
        // Given
        val refreshToken = jwtService.generateRefreshToken(testUser, testTenant)

        // When & Then
        assertTrue(jwtService.isRefreshToken(refreshToken))
    }

    @Test
    fun `isRefreshToken should return false for access token`() {
        // Given
        val accessToken = jwtService.generateToken(testUser, testTenant)

        // When & Then
        assertFalse(jwtService.isRefreshToken(accessToken))
    }

    @Test
    fun `isRefreshToken should return false for verification token`() {
        // Given
        val verificationToken = jwtService.generateVerificationToken(testUser.email, testTenant.name)

        // When & Then
        assertFalse(jwtService.isRefreshToken(verificationToken))
    }

    @Test
    fun `isVerificationToken should return true for verification token`() {
        // Given
        val verificationToken = jwtService.generateVerificationToken(testUser.email, testTenant.name)

        // When & Then
        assertTrue(jwtService.isVerificationToken(verificationToken))
    }

    @Test
    fun `isVerificationToken should return false for access token`() {
        // Given
        val accessToken = jwtService.generateToken(testUser, testTenant)

        // When & Then
        assertFalse(jwtService.isVerificationToken(accessToken))
    }

    @Test
    fun `isVerificationToken should return false for refresh token`() {
        // Given
        val refreshToken = jwtService.generateRefreshToken(testUser, testTenant)

        // When & Then
        assertFalse(jwtService.isVerificationToken(refreshToken))
    }

    @Test
    fun `getExpirationDate should return correct expiration date`() {
        // Given
        val token = jwtService.generateToken(testUser, testTenant)
        val currentTime = System.currentTimeMillis()

        // When
        val expirationDate = jwtService.getExpirationDate(token)

        // Then
        assertNotNull(expirationDate)
        // Should be approximately current time + expiration (86400000ms = 24 hours)
        val expectedExpiration = currentTime + 86400000
        val tolerance = 1000 // 1 second tolerance
        assertTrue(abs(expirationDate.time - expectedExpiration) < tolerance)
    }

    @Test
    fun `tokens should contain correct claims`() {
        // Given
        val accessToken = jwtService.generateToken(testUser, testTenant)
        val refreshToken = jwtService.generateRefreshToken(testUser, testTenant)
        val verificationToken = jwtService.generateVerificationToken(testUser.email, testTenant.name)

        // When & Then - Access Token
        assertEquals(testUser.id, jwtService.extractUserId(accessToken))
        assertEquals(testUser.email, jwtService.extractEmail(accessToken))
        assertEquals(testTenant.id, jwtService.extractTenantId(accessToken))
        assertEquals(testTenant.name, jwtService.extractTenantName(accessToken))
        assertEquals(testUser.roles.map { it.name }, jwtService.extractRoles(accessToken))

        // When & Then - Refresh Token
        assertEquals(testUser.id, jwtService.extractUserId(refreshToken))
        assertEquals(testUser.email, jwtService.extractEmail(refreshToken))
        assertEquals(testTenant.id, jwtService.extractTenantId(refreshToken))
        assertEquals(testTenant.name, jwtService.extractTenantName(refreshToken))

        // When & Then - Verification Token
        val verificationData = jwtService.parseVerificationToken(verificationToken)
        assertNotNull(verificationData)
        assertEquals(testUser.email, verificationData?.email)
        assertEquals(testTenant.name, verificationData?.tenantName)
    }

    @Test
    fun `parseVerificationToken should return null for expired verification token`() {
        // Given - create service with very short expiration
        val shortExpirationService = JwtService()
        ReflectionTestUtils.setField(shortExpirationService, "secret", "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtYXQtbGVhc3QtMjU2LWJpdHMtbG9uZy1mb3ItaHMyNTY=")
        ReflectionTestUtils.setField(shortExpirationService, "verificationExpiration", 1L) // 1ms

        val verificationToken = shortExpirationService.generateVerificationToken(testUser.email, testTenant.name)

        // Wait for token to expire
        Thread.sleep(10)

        // When
        val tokenData = shortExpirationService.parseVerificationToken(verificationToken)

        // Then
        assertNull(tokenData)
    }

    @Test
    fun `extracting claims from invalid token should throw exception`() {
        // Given
        val invalidToken = "invalid.token.here"

        // When & Then
        assertThrows<MalformedJwtException> {
            jwtService.extractUserId(invalidToken)
        }

        assertThrows<MalformedJwtException> {
            jwtService.extractEmail(invalidToken)
        }

        assertThrows<MalformedJwtException> {
            jwtService.extractTenantId(invalidToken)
        }
    }

    @Test
    fun `different token types should have different signatures when created at same time`() {
        // Given & When
        val accessToken = jwtService.generateToken(testUser, testTenant)
        val refreshToken = jwtService.generateRefreshToken(testUser, testTenant)
        val verificationToken = jwtService.generateVerificationToken(testUser.email, testTenant.name)

        // Then
        // All tokens should be different (different claims and expiration times)
        assertNotEquals(accessToken, refreshToken)
        assertNotEquals(accessToken, verificationToken)
        assertNotEquals(refreshToken, verificationToken)
    }

    @Test
    fun `tokens should be valid immediately after creation`() {
        // Given & When
        val accessToken = jwtService.generateToken(testUser, testTenant)
        val refreshToken = jwtService.generateRefreshToken(testUser, testTenant)
        val verificationToken = jwtService.generateVerificationToken(testUser.email, testTenant.name)

        // Then
        assertTrue(jwtService.isTokenValid(accessToken))
        assertTrue(jwtService.isTokenValid(refreshToken))
        assertTrue(jwtService.isTokenValid(verificationToken))
    }
}