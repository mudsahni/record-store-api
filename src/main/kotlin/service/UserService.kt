package com.muditsahni.service

import com.muditsahni.model.entity.User
import com.muditsahni.repository.global.TenantRepository
import com.muditsahni.repository.TenantAwareUserRepository

interface UserService {

    /**
     * Repository for managing user entities.
     */
    val tenantAwareUserRepository: TenantAwareUserRepository

    /**
     * Repository for managing tenant entities.
     */
    val tenantRepository: TenantRepository

    /**
     * Retrieves a user by their email.
     * @param email The email of the user to retrieve.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return The [User] object if found, or null if not found.
     */
    suspend fun getUserByEmail(
        email: String,
        tenantName: String
    ): User?

    /**
     * Retrieves a user by their phone number.
     * @param phoneNumber The phone number of the user to retrieve.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return The [User] object if found, or null if not found.
     */
    suspend fun getUserByPhoneNumber(
        phoneNumber: String,
        tenantName: String
    ): User?

    /**
     * Deactivates a user by their email.
     * This method updates the user's `isActive` flag to false
     * instead of removing the user from the database.
     * @param email The email of the user to deactivate.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return True if the user was successfully deactivated, false if the user does not exist or is already inactive.
     */
    suspend fun deactivateUserByEmail(
        email: String,
        tenantName: String
    ): Boolean

    /**
     * Deactivates a user by their phone number.
     * This method updates the user's `isActive` flag to false
     * instead of removing the user from the database.
     * @param phoneNumber The phone number of the user to deactivate.
     * @param tenantName The name of the tenant to which the user belongs.
     * @return True if the user was successfully deactivated, false if the user does not exist or is already inactive.
     */
    suspend fun deactivateUserByPhoneNumber(
        phoneNumber: String,
        tenantName: String
    ): Boolean

    /**
     * Changes the password for a user.
     * @param email The email of the user whose password is to be changed.
     * @param tenantName The name of the tenant to which the user belongs.
     * @param oldPassword The current password of the user.
     * @param newPassword The new password to set for the user.
     * @return True if the password was successfully changed, false otherwise.
     */
    suspend fun changePassword(
        email: String,
        tenantName: String,
        oldPassword: String,
        newPassword: String
    ): Boolean
}