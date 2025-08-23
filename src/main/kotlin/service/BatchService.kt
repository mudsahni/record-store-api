package com.muditsahni.service

import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.repository.BatchRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BatchService {
    val batchRepository: BatchRepository

    suspend fun createBatch(
        tenant: Tenant,
        name: String,
        type: BatchType,
        tags: Map<String, String>,
        user: User
    ): Batch

    /**
     * Retrieves all batches for the current tenant.
     */
    suspend fun getAllBatches(): Flow<Batch>

    /**
     * Retrieves batches by status for the current tenant.
     */
    suspend fun getBatchesByStatus(status: BatchStatus): Flow<Batch>

    /**
     * Retrieves batches created by a specific user for the current tenant.
     */
    suspend fun getBatchesCreatedByUser(userEmail: String): Flow<Batch>

    /**
     * Retrieves a specific batch by ID for the current tenant.
     */
    suspend fun getBatchById(id: UUID): Batch?

    /**
     * Updates batch status.
     */
    suspend fun updateBatchStatus(batchId: UUID, newStatus: BatchStatus, updatedBy: User): Batch?
}