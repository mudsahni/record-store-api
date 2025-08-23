package com.muditsahni.service.v1

import com.muditsahni.constant.General
import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import com.muditsahni.model.entity.Tenant
import com.muditsahni.model.entity.User
import com.muditsahni.repository.BatchRepository
import com.muditsahni.service.BatchService
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DefaultBatchService(
    override val batchRepository: BatchRepository,
): BatchService {

    /**
     * Creates a new batch for the given tenant with the specified name and type.
     * This method initializes the batch with a status of CREATED,
     * sets the createdBy field to a system user,
     * and assigns the owner to the system user.
     * @param tenant The tenant for which the batch is being created.
     * @param name The name of the batch.
     * @param type The type of the batch (e.g., import, export).
     * @param tags Optional tags associated with the batch.
     * @param user The user creating the batch.
     * @return The created [Batch] object with initial properties set.
     */
    override suspend fun createBatch(
        tenant: Tenant,
        name: String,
        type: BatchType,
        tags: Map<String, String>,
        user: User
    ): Batch {
        val batch = Batch(
            name = name,
            tenantName = tenant.name,
            type = type,
            status = BatchStatus.CREATED,
            createdBy = user.email,
            tags = tags,
            owners = listOf(user.email),
        )
        return batchRepository.save(batch)
    }

    /**
     * Retrieves all batches for the current tenant.
     * @return A [Flow] of [Batch] objects representing all batches for the tenant.
     */
    override suspend fun getAllBatches(): Flow<Batch> {
        return batchRepository.findAll()
    }

    /**
     * Retrieves batches by status for the current tenant.
     * @param status The status of the batches to retrieve (e.g., CREATED, PROCESSING, COMPLETED).
     * @return A [Flow] of [Batch] objects matching the specified status.
     */
    override suspend fun getBatchesByStatus(status: BatchStatus): Flow<Batch> {
        return batchRepository.findByStatus(status)
    }

    /**
    * Retrieves batches created by a specific user for the current tenant.
    * @param userEmail The email of the user whose created batches are to be retrieved.
    * @return A [Flow] of [Batch] objects created by the specified user.
     */
    override suspend fun getBatchesCreatedByUser(userEmail: String): Flow<Batch> {
        return batchRepository.findByCreatedBy(userEmail)
    }

    /**
     * Retrieves a specific batch by ID for the current tenant.
     * @param id The UUID of the batch to retrieve.
     * @return The [Batch] object if found, or null if not found.
     */
    override suspend fun getBatchById(id: UUID): Batch? {
        return batchRepository.findById(id)
    }

    /**
     * Updates batch status.
     * @param batchId The UUID of the batch to update.
     * @param newStatus The new status to set for the batch.
     * @param updatedBy The user performing the status update.
     * @return The updated [Batch] object if the batch exists, or null if not found.
     */
    override suspend fun updateBatchStatus(batchId: UUID, newStatus: BatchStatus, updatedBy: User): Batch? {
        val batch = batchRepository.findById(batchId) ?: return null

        val updatedBatch = batch.copy(
            status = newStatus,
            updatedBy = updatedBy.email,
            updatedAt = Instant.now()
        )

        return batchRepository.save(updatedBatch)
    }
}
