package com.muditsahni.repository

import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

import com.muditsahni.model.entity.Record

@Repository
interface BatchRepository : CoroutineCrudRepository<Batch, UUID> {

    // Basic queries with Flow return type
    fun findByStatus(status: BatchStatus): Flow<Batch>
    fun findByType(type: BatchType): Flow<Batch>

    // Find records by creation date range
    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): Flow<Record>

    // Count queries
    suspend fun countByStatus(status: BatchStatus): Long

}