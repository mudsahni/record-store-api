package com.muditsahni.service.v1

import com.muditsahni.constant.General
import com.muditsahni.error.UserAlreadyExistsException
import com.muditsahni.model.entity.Role
import com.muditsahni.model.entity.User
import com.muditsahni.repository.TenantRepository
import com.muditsahni.repository.UserRepository
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DefaultUserService(
    override val userRepository: UserRepository,
    override val tenantRepository: TenantRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    /**
     * Creates a new user with the specified email and phone number.
     * @param email The email of the user.
     * @param tenantName The name of the tenant to which the user belongs.
     * @param phoneNumber The phone number of the user.
     * @param firstName The first name of the user (optional).
     * @param lastName The last name of the user (optional).
     * @param password The password for the user (optional, for auth users).
     * @param roles The roles for the user.
     * @return The created [User] object.
     */
    private suspend fun createUser(
        email: String,
        tenantName: String,
        phoneNumber: String,
        firstName: String?,
        lastName: String?,
        password: String? = null,
        roles: List<Role> = listOf(Role.USER)
    ): User {
        val tenant = tenantRepository.findByName(tenantName)
            ?: throw IllegalArgumentException("Tenant with name $tenantName does not exist.")

        if (tenant.deleted) {
            throw IllegalArgumentException("Tenant $tenantName is inactive.")
        }

        if (userRepository.findByEmail(email) != null) {
            throw UserAlreadyExistsException("email", email)
        }

        if (userRepository.findByPhoneNumber(phoneNumber) != null) {
            throw UserAlreadyExistsException("phoneNumber", phoneNumber)
        }

        val createdBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        val user = User(
            email = email,
            phoneNumber = phoneNumber,
            tenantName = tenant.name,
            firstName = firstName,
            lastName = lastName,
            createdBy = createdBy,
            passwordHash = password?.let { passwordEncoder.encode(it) } ?: "",
            roles = roles,
        )
        return userRepository.save(user)
    }

    /**
     * Creates a new authentication user with the specified email, phone number, and password.
     * This method is specifically for creating users that will be used for authentication purposes.
     * @param email The email of the user.
     * @param tenantName The name of the tenant to which the user belongs.
     * @param phoneNumber The phone number of the user.
     * @param password The password for the user.
     * @param firstName The first name of the user (optional).
     * @param lastName The last name of the user (optional).
     * @param roles The roles for the user (default is ["USER"]).
     * @return The created [User] object.
     * @throws IllegalArgumentException if the password is blank.
     */
    override suspend fun createAuthUser(
        email: String,
        tenantName: String,
        phoneNumber: String,
        password: String,
        firstName: String?,
        lastName: String?,
        roles: List<Role>
    ): User {
        if (password.isBlank()) {
            throw IllegalArgumentException("Password cannot be empty for auth users")
        }

        return createUser(email, tenantName, phoneNumber, firstName, lastName, password, roles)
    }


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

        val user = userRepository.findByPhoneNumber(phoneNumber)
        if (user != null && user.tenantName == tenant.name && user.isActive) {
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

        val user = userRepository.findByEmail(email)
        if (user != null && user.tenantName == tenant.name && user.isActive) {
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

        if (!user.isActive) {
            return false
        }

        val updatedBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        user.isActive = false
        user.updatedAt = Instant.now()
        user.updatedBy = updatedBy
        userRepository.save(user)
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

        if (!user.isActive) {
            return false
        }

        val updatedBy = try {
            CoroutineSecurityUtils.getCurrentUserEmail() ?: General.SYSTEM.toString()
        } catch (e: Exception) {
            General.SYSTEM.toString()
        }

        user.isActive = false
        user.updatedAt = Instant.now()
        user.updatedBy = updatedBy
        userRepository.save(user)
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

        userRepository.save(user)
        return true
    }

    /**
     * Finds a user by email for authentication purposes (includes password hash).
     */
    suspend fun findUserForAuthentication(email: String): User? {
        return userRepository.findByEmail(email)
    }
}