package com.muditsahni.service.command

import java.util.UUID

data class UploadRequestResult(
    val recordId: UUID,
    val uploadUrl: String
)