package com.muditsahni.service

import com.mongodb.reactivestreams.client.MongoClient
import com.muditsahni.config.TenantContext
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service

@Service
@Primary
class TenantAwareMongoService(
    private val mongoClient: MongoClient
) {
    private val templateCache = mutableMapOf<String, ReactiveMongoTemplate>()

    fun getTemplateForTenant(tenantName: String): ReactiveMongoTemplate {
        return templateCache.getOrPut(tenantName) {
            val databaseName = "tenant-${tenantName}-db"
            ReactiveMongoTemplate(mongoClient, databaseName)
        }
    }

    fun getCurrentTenantTemplate(): ReactiveMongoTemplate {
        val tenant = TenantContext.Companion.getTenant()
            ?: throw IllegalStateException("No tenant context set")
        return getTemplateForTenant(tenant.name)
    }
}