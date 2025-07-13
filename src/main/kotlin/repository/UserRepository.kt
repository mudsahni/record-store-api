package com.muditsahni.repository

import com.muditsahni.model.entity.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : CoroutineCrudRepository<User, UUID> {

    /**
     * Finds a user by their email.
     * @param email The email of the user to search for.
     * @return The [User] object if found, or null if not found.
     */
    suspend fun findByEmail(email: String): User?

    /**
     * Finds a user by their phone number.
     * @param phoneNumber The phone number of the user to search for.
     * @return The [User] object if found, or null if not found.
     */
    suspend fun findByPhoneNumber(phoneNumber: String): User?
}