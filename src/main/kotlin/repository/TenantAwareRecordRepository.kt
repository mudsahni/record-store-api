package com.muditsahni.repository

import com.muditsahni.model.entity.Record
import com.muditsahni.model.entity.RecordStatus
import com.muditsahni.model.entity.RecordType
import com.muditsahni.service.TenantAwareMongoService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
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
class TenantAwareRecordRepository(
    private val tenantAwareMongoService: TenantAwareMongoService
) : RecordRepository {

    override suspend fun save(record: Record): Record {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()

        record.updatedAt = Instant.now()

        return template.save(record).awaitFirst()
    }

    override suspend fun findById(id: UUID): Record? {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("id").`is`(id))
        return template.findOne(query, Record::class.java).awaitFirstOrNull()
    }

    override suspend fun findAll(): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        return template.findAll(Record::class.java).asFlow()
    }

    override suspend fun deleteById(id: UUID) {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("id").`is`(id))
        template.remove(query, Record::class.java).awaitFirst()
    }

    override suspend fun delete(record: Record) {
        deleteById(record.id)
    }

    override suspend fun existsById(id: UUID): Boolean {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("id").`is`(id))
        return template.exists(query, Record::class.java).awaitFirst()
    }

    override suspend fun count(): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        return template.count(Query(), Record::class.java).awaitFirst()
    }

    override fun findByBatchId(batchId: UUID): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("batchId").`is`(batchId))
        return template.find(query, Record::class.java).asFlow()
    }

    override fun findByBatchIdAndStatus(
        batchId: UUID,
        status: RecordStatus
    ): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(
            Criteria.where("batchId").`is`(batchId)
                .and("status").`is`(status)
        )
        return template.find(query, Record::class.java).asFlow()
    }

    override fun findByBatchIdAndType(
        batchId: UUID,
        type: RecordType
    ): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(
            Criteria.where("batchId").`is`(batchId)
                .and("type").`is`(type)
        )
        return template.find(query, Record::class.java).asFlow()
    }

    override fun findByStatus(status: RecordStatus): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("status").`is`(status))
        return template.find(query, Record::class.java).asFlow()
    }

    override fun findByType(type: RecordType): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("type").`is`(type))
        return template.find(query, Record::class.java).asFlow()
    }

    override fun findByTenantName(tenantName: String): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("tenantName").`is`(tenantName))
        return template.find(query, Record::class.java).asFlow()
    }

    override fun findByCreatedBy(createdBy: String): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("createdBy").`is`(createdBy))
        return template.find(query, Record::class.java).asFlow()
    }

    override fun findByBatchIdAndStatusAndDocumentType(
        batchId: UUID,
        status: RecordStatus,
        documentType: String
    ): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(
            Criteria.where("batchId").`is`(batchId)
                .and("status").`is`(status)
                .and("documentType").`is`(documentType)
        )
        return template.find(query, Record::class.java).asFlow()
    }

    override suspend fun deleteByBatchId(batchId: UUID): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("batchId").`is`(batchId))
        val result = template.remove(query, Record::class.java).awaitFirst()
        return result.deletedCount
    }

    override fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): Flow<Record> {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(
            Criteria.where("createdAt").gte(startDate).lte(endDate)
        )
        return template.find(query, Record::class.java).asFlow()
    }

    override suspend fun countByBatchId(batchId: UUID): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("batchId").`is`(batchId))
        return template.count(query, Record::class.java).awaitFirst()
    }

    override suspend fun countByBatchIdAndStatus(
        batchId: UUID,
        status: RecordStatus
    ): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(
            Criteria.where("batchId").`is`(batchId)
                .and("status").`is`(status)
        )
        return template.count(query, Record::class.java).awaitFirst()
    }

    override suspend fun countByBatchIdAndType(
        batchId: UUID,
        type: RecordType
    ): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(
            Criteria.where("batchId").`is`(batchId)
                .and("type").`is`(type)
        )
        return template.count(query, Record::class.java).awaitFirst()
    }

    override suspend fun countByStatus(status: RecordStatus): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("status").`is`(status))
        return template.count(query, Record::class.java).awaitFirst()
    }

    override suspend fun countByType(type: RecordType): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("type").`is`(type))
        return template.count(query, Record::class.java).awaitFirst()
    }

    override suspend fun countByTenantName(tenantName: String): Long {
        val template = tenantAwareMongoService.getCurrentTenantTemplate()
        val query = Query.query(Criteria.where("tenantName").`is`(tenantName))
        return template.count(query, Record::class.java).awaitFirst()
    }
}