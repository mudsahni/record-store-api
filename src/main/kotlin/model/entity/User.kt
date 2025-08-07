package com.muditsahni.model.entity

import com.muditsahni.model.enums.UserStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

/**
 * User entity representing a user in the system.
 * It includes personal information, roles, and status.
 * The `User` class is used to manage user accounts in the system.
 * It is stored in a MongoDB collection named "users".
 * @Document(collection = "users")
 * @property id Unique identifier for the user.
 * @property firstName First name of the user.
 * @property lastName Last name of the user.
 * @property tenantName Identifier/Name for the tenant to which the user belongs.
 * @property email Email address of the user.
 * @property phoneNumber Phone number of the user.
 * @property passwordHash Hashed password for the user account.
 * @property status Current status of the user account (e.g., active, inactive, suspended, pending).
 * @property roles List of roles assigned to the user (e.g., admin, user).
 * @property createdAt Timestamp when the user account was created.
 * @property updatedAt Timestamp when the user account was last updated.
 * @property updatedBy Identifier of the user who last updated this account.
 * @property lastLoginAt Timestamp of the user's last login.
 * @property failedLoginAttempts Number of failed login attempts.
 * @property accountLockedUntil Timestamp until which the account is locked due to failed login attempts.
 * @property passwordChangedAt Timestamp when the password was last changed.
 * @property mustChangePassword Flag indicating whether the user must change their password on next login.
 */
@Document("users")
data class User(
    @Id
    val id: UUID = UUID.randomUUID(),
    var firstName: String,
    var lastName: String,
    val tenantName: String,
    @Indexed(unique = true)
    var email: String,
    @Indexed(unique = true)
    var phoneNumber: String,
    var passwordHash: String,
    var status: UserStatus,
    val roles: List<Role> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val createdBy: String,
    var updatedAt: Instant? = null,
    var updatedBy: String? = null,
    var lastLoginAt: Instant? = null,
    var failedLoginAttempts: Int = 0,
    var accountLockedUntil: Instant? = null,
    var passwordChangedAt: Instant = Instant.now(),
    var mustChangePassword: Boolean = false,
    val verificationToken: String? = null,
    val verificationTokenExpiresAt: Instant? = null,
    val emailVerified: Boolean = false,
    ) {

    companion object {
        const val MAX_FAILED_LOGIN_ATTEMPTS = 5
    }

    /**
     * Checks if the user account is currently locked.
     * The account is considered locked if the `accountLockedUntil` timestamp
     * is set and is in the future.
     * @return Boolean indicating whether the account is locked.
     */
    fun isAccountLocked(): Boolean {
        return accountLockedUntil?.isAfter(Instant.now()) == true
    }

    /**
     * Checks if the account should be locked based on the number of failed login attempts.
     * If the number of failed attempts is 5 or more, the account should be locked.
     * @return Boolean indicating whether the account should be locked.
     */
    fun shouldLockAccount(): Boolean {
        return failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS
    }

}
