package com.muditsahni.repository

import com.muditsahni.model.entity.User
import com.muditsahni.service.TenantAwareMongoService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TenantAwareUserRepository(
    private val tenantAwareMongoService: TenantAwareMongoService
) {

    suspend fun save(user: User): User {
        return tenantAwareMongoService.getCurrentTenantTemplate()
            .save(user)
            .awaitSingle()
    }

    suspend fun findById(id: UUID): User? {
        return tenantAwareMongoService.getCurrentTenantTemplate()
            .findById(id, User::class.java)
            .awaitSingleOrNull()
    }

    suspend fun findByEmail(email: String): User? {
        return tenantAwareMongoService.getCurrentTenantTemplate()
            .findOne(Query.query(Criteria.where("email").`is`(email)), User::class.java)
            .awaitSingleOrNull()
    }

    suspend fun findByPhoneNumber(phoneNumber: String): User? {
        return tenantAwareMongoService.getCurrentTenantTemplate()
            .findOne(Query.query(Criteria.where("phoneNumber").`is`(phoneNumber)), User::class.java)
            .awaitSingleOrNull()
    }

    suspend fun existsByEmail(email: String): Boolean {
        return tenantAwareMongoService.getCurrentTenantTemplate()
            .exists(Query.query(Criteria.where("email").`is`(email)), User::class.java)
            .awaitSingle()
    }

    suspend fun findByVerificationToken(token: String): User? {
        return tenantAwareMongoService.getCurrentTenantTemplate()
            .findOne(Query.query(Criteria.where("verificationToken").`is`(token)), User::class.java)
            .awaitSingleOrNull()
    }

    // For operations without JWT context (like email verification)
    suspend fun findByVerificationTokenInTenant(token: String, tenantName: String): User? {
        return tenantAwareMongoService.getTemplateForTenant(tenantName)
            .findOne(Query.query(Criteria.where("verificationToken").`is`(token)), User::class.java)
            .awaitSingleOrNull()
    }

    suspend fun findByEmailInTenant(email: String, tenantName: String): User? {
        return tenantAwareMongoService.getTemplateForTenant(tenantName)
            .findOne(Query.query(Criteria.where("email").`is`(email)), User::class.java)
            .awaitSingleOrNull()
    }
}