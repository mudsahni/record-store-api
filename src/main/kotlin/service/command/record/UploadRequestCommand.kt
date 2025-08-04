package com.muditsahni.service.command.record

import com.muditsahni.model.entity.RecordType
import java.util.UUID

data class UploadRequestCommand(
    val batchId: UUID,
    val fileName: String,
    val type: RecordType,
    val createdBy: String
)