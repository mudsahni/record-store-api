package com.muditsahni.repository

import com.muditsahni.model.entity.Record
import com.muditsahni.model.entity.RecordStatus
import com.muditsahni.model.entity.RecordType
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface RecordRepository {

    // Basic CRUD operations
    suspend fun save(record: Record): Record
    suspend fun findById(id: UUID): Record?
    suspend fun findAll(): Flow<Record>
    suspend fun deleteById(id: UUID)
    suspend fun delete(record: Record)
    suspend fun existsById(id: UUID): Boolean
    suspend fun count(): Long

    // Custom queries with Flow return type
    fun findByBatchId(batchId: UUID): Flow<Record>
    fun findByBatchIdAndStatus(batchId: UUID, status: RecordStatus): Flow<Record>
    fun findByBatchIdAndType(batchId: UUID, type: RecordType): Flow<Record>
    fun findByStatus(status: RecordStatus): Flow<Record>
    fun findByType(type: RecordType): Flow<Record>
    fun findByTenantName(tenantName: String): Flow<Record>
    fun findByCreatedBy(createdBy: String): Flow<Record>
    fun findByBatchIdAndStatusAndDocumentType(batchId: UUID, status: RecordStatus, documentType: String): Flow<Record>
    suspend fun deleteByBatchId(batchId: UUID): Long

    // Find records by creation date range
    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): Flow<Record>

    // Count queries
    suspend fun countByBatchId(batchId: UUID): Long
    suspend fun countByBatchIdAndStatus(batchId: UUID, status: RecordStatus): Long
    suspend fun countByBatchIdAndType(batchId: UUID, type: RecordType): Long
    suspend fun countByStatus(status: RecordStatus): Long
    suspend fun countByType(type: RecordType): Long
    suspend fun countByTenantName(tenantName: String): Long
}

