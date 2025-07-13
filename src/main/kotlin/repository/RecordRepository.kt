package com.muditsahni.repository

import kotlinx.coroutines.flow.Flow
import com.muditsahni.models.entity.RecordStatus
import com.muditsahni.models.entity.RecordType
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

import com.muditsahni.models.entity.Record

@Repository
interface RecordRepository : CoroutineCrudRepository<Record, UUID> {

    // Basic queries with Flow return type
    fun findByBatchId(batchId: UUID): Flow<Record>
    fun findByStatus(status: RecordStatus): Flow<Record>
    fun findByType(type: RecordType): Flow<Record>

    // Find records by creation date range
    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): Flow<Record>
    // Find records by multiple criteria
    fun findByBatchIdAndStatus(batchId: UUID, status: RecordStatus): Flow<Record>

    // Custom query example
    @Query("{'batchId': ?0, 'status': ?1, 'documentType': ?2}")
    fun findByBatchIdStatusAndDocumentType(
        batchId: UUID,
        status: String,
        documentType: String
    ): Flow<Record>

    // Count queries
    suspend fun countByStatus(status: RecordStatus): Long
    suspend fun countByBatchId(batchId: UUID): Long

    // Delete operations
    suspend fun deleteByBatchId(batchId: UUID): Long

}