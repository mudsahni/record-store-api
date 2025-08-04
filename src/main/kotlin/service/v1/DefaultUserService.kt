package com.muditsahni.service.v1

import com.muditsahni.constant.General
import com.muditsahni.model.entity.User
import com.muditsahni.model.enums.UserStatus
import com.muditsahni.repository.global.TenantRepository
import com.muditsahni.repository.TenantAwareUserRepository
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DefaultUserService(
    override val tenantAwareUserRepository: TenantAwareUserRepository,
    override val tenantRepository: TenantRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {



    /**
     * Retrieves a user by their email.
     * @param phoneNumber The phoneNumber of the user to retrieve.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return The [User] object if found, or null if not found.
     */
    override suspend fun getUserByPhoneNumber(
        phoneNumber: String,
        tenantName: String
    ): User? {
        val tenant = tenantRepository.findByName(tenantName)
            ?: throw IllegalArgumentException("Tenant with name $tenantName does not exist.")

        if (tenant.deleted) {
            return null
        }

        val user = tenantAwareUserRepository.findByPhoneNumber(phoneNumber)
        if (user != null && user.tenantName == tenant.name) {
            return user
        }
        return null
    }

    /**
     * Retrieves a user by their email.
     * @param email The email of the user to retrieve.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return The [User] object if found, or null if not found.
     */
    override suspend fun getUserByEmail(
        email: String,
        tenantName: String
    ): User? {
        val tenant = tenantRepository.findByName(tenantName)
            ?: throw IllegalArgumentException("Tenant with name $tenantName does not exist.")

        if (tenant.deleted) {
            return null
        }

        val user = tenantAwareUserRepository.findByEmail(email)
        if (user != null && user.tenantName == tenant.name) {
            return user
        }
        return null
    }

    /**
     * Deactivates a user by their email.
     * This method updates the user's `isActive` flag to false
     * instead of removing the user from the database.
     * @param email The email of the user to deactivate.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return True if the user was successfully deactivated, false if the user does not exist or is already inactive.
     */
    override suspend fun deactivateUserByEmail(
        email: String,
        tenantName: String
    ): Boolean {
        val tenant = tenantRepository.findByName(tenantName)
            ?: throw IllegalArgumentException("Tenant with name $tenantName does not exist.")

        val user = getUserByEmail(email, tenant.name) ?: return false

        if (user.status != UserStatus.ACTIVE) {
            return false
        }

        val updatedBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        user.status = UserStatus.INACTIVE
        user.updatedAt = Instant.now()
        user.updatedBy = updatedBy
        tenantAwareUserRepository.save(user)
        return true
    }

    /**
     * Deactivates a user by their phone number.
     * This method updates the user's `isActive` flag to false
     * instead of removing the user from the database.
     * @param phoneNumber The phone number of the user to deactivate.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return True if the user was successfully deactivated, false if the user does not exist or is already inactive.
     */
    override suspend fun deactivateUserByPhoneNumber(
        phoneNumber: String,
        tenantName: String
    ): Boolean {
        val tenant = tenantRepository.findByName(tenantName)
            ?: throw IllegalArgumentException("Tenant with name $tenantName does not exist.")

        val user = getUserByPhoneNumber(phoneNumber, tenant.name) ?: return false

        if (user.status != UserStatus.ACTIVE) {
            return false
        }

        val updatedBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        user.status = UserStatus.INACTIVE
        user.updatedAt = Instant.now()
        user.updatedBy = updatedBy
        tenantAwareUserRepository.save(user)
        return true
    }

    /**
     * Changes a user's password.
     * @param email The email of the user.
     * @param tenantName The tenant name.
     * @param oldPassword The current password.
     * @param newPassword The new password.
     * @return True if password was changed successfully.
     */
    override suspend fun changePassword(
        email: String,
        tenantName: String,
        oldPassword: String,
        newPassword: String
    ): Boolean {
        val user = getUserByEmail(email, tenantName) ?: return false

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.passwordHash)) {
            return false
        }

        // Get current user for audit trail
        val updatedBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        user.passwordHash = passwordEncoder.encode(newPassword)
        user.passwordChangedAt = Instant.now()
        user.mustChangePassword = false
        user.updatedAt = Instant.now()
        user.updatedBy = updatedBy

        tenantAwareUserRepository.save(user)
        return true
    }

    /**
     * Finds a user by email for authentication purposes (includes password hash).
     */
    suspend fun findUserForAuthentication(email: String): User? {
        return tenantAwareUserRepository.findByEmail(email)
    }
}