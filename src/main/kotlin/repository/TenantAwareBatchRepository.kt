package com.muditsahni.repository

import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import com.muditsahni.service.TenantAwareMongoService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

// Tenant-aware implementation
@Repository
class TenantAwareBatchRepository(
    private val tenantAwareMongoService: TenantAwareMongoService
) : BatchRepository {

    override suspend fun save(batch: Batch): Batch {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()

        batch.updatedAt = Instant.now()

        return template.save(batch).awaitFirst()
    }

    override suspend fun findById(id: UUID): Batch? {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("id").`is`(id))
        return template.findOne(query, Batch::class.java).awaitFirstOrNull()
    }

    override suspend fun findAll(): Flow<Batch> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        return template.findAll(Batch::class.java).asFlow()
    }

    override suspend fun deleteById(id: UUID) {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("id").`is`(id))
        template.remove(query, Batch::class.java).awaitFirst()
    }

    override suspend fun delete(batch: Batch) {
        batch.id?.let { deleteById(it) }
    }

    override suspend fun existsById(id: UUID): Boolean {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("id").`is`(id))
        return template.exists(query, Batch::class.java).awaitFirst()
    }

    override suspend fun count(): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        return template.count(Query(), Batch::class.java).awaitFirst()
    }

    override fun findByStatus(status: BatchStatus): Flow<Batch> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("status").`is`(status))
        return template.find(query, Batch::class.java).asFlow()
    }

    override fun findByType(type: BatchType): Flow<Batch> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("type").`is`(type))
        return template.find(query, Batch::class.java).asFlow()
    }

    override fun findByTenantName(tenantName: String): Flow<Batch> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("tenantName").`is`(tenantName))
        return template.find(query, Batch::class.java).asFlow()
    }

    override fun findByCreatedBy(createdBy: String): Flow<Batch> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("createdBy").`is`(createdBy))
        return template.find(query, Batch::class.java).asFlow()
    }

    override fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): Flow<Batch> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(
            Criteria.where("createdAt").gte(startDate).lte(endDate)
        )
        return template.find(query, Batch::class.java).asFlow()
    }

    override suspend fun countByStatus(status: BatchStatus): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("status").`is`(status))
        return template.count(query, Batch::class.java).awaitFirst()
    }

    override suspend fun countByType(type: BatchType): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("type").`is`(type))
        return template.count(query, Batch::class.java).awaitFirst()
    }

    override suspend fun countByTenantName(tenantName: String): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("tenantName").`is`(tenantName))
        return template.count(query, Batch::class.java).awaitFirst()
    }
}