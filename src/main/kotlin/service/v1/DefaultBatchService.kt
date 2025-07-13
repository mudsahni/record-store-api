package com.muditsahni.service.v1

import com.muditsahni.constant.General
import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchStatus
import com.muditsahni.model.entity.BatchType
import com.muditsahni.model.entity.Tenant
import com.muditsahni.repository.BatchRepository
import com.muditsahni.service.BatchService
import org.springframework.stereotype.Service

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
     * @return The created [Batch] object with initial properties set.
     */
    override suspend fun createBatch(
        tenant: Tenant,
        name: String,
        type: BatchType,
    ): Batch {
        // TODO: replace with actual user
        val user = General.SYSTEM.toString()
        val batch = Batch(
            name = name,
            tenantName = tenant.name,
            type = type,
            status = BatchStatus.CREATED,
            createdBy = user,
            owners = listOf(user),
        )
        return batchRepository.save(batch)
    }
}
