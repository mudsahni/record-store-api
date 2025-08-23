package com.muditsahni.repository

import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import com.muditsahni.model.entity.Record
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

// Interface remains the same, but implementation will be tenant-aware
@Repository
interface BatchRepository {

    // Basic CRUD operations
    suspend fun save(batch: Batch): Batch
    suspend fun findById(id: UUID): Batch?
    suspend fun findAll(): Flow<Batch>
    suspend fun deleteById(id: UUID)
    suspend fun delete(batch: Batch)
    suspend fun existsById(id: UUID): Boolean
    suspend fun count(): Long

    // Custom queries with Flow return type
    fun findByStatus(status: BatchStatus): Flow<Batch>
    fun findByType(type: BatchType): Flow<Batch>
    fun findByTenantName(tenantName: String): Flow<Batch>
    fun findByCreatedBy(createdBy: String): Flow<Batch>

    // Find records by creation date range
    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): Flow<Batch>

    // Count queries
    suspend fun countByStatus(status: BatchStatus): Long
    suspend fun countByType(type: BatchType): Long
    suspend fun countByTenantName(tenantName: String): Long
}

