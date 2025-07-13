package com.muditsahni.service

import com.muditsahni.model.entity.Batch
import com.muditsahni.model.entity.BatchType
import com.muditsahni.model.entity.Tenant
import com.muditsahni.repository.BatchRepository

interface BatchService {
    val batchRepository: BatchRepository

    suspend fun createBatch(
        tenant: Tenant,
        name: String,
        type: BatchType,
    ): Batch
}