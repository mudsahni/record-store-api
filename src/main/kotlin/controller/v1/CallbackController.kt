package com.muditsahni.controller.v1

import com.muditsahni.model.entity.Role
import com.muditsahni.security.CoroutineSecurityUtils
import com.muditsahni.service.v1.DefaultRecordService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class CallbackController(
    private val recordService: DefaultRecordService
) {

//    @PutMapping("/upload/complete")
//    suspend fun completeUpload(
//        @PathVariable tenantName: String,
//        @PathVariable recordId: UUID
//    ): ResponseEntity<Void>  {
//        val userTenant = CoroutineSecurityUtils.getCurrentTenant()
//
//        if (userTenant?.name != tenantName) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
//        }
//
//        if (!CoroutineSecurityUtils.hasAnyRole(Role.USER, Role.ADMIN, Role.SUPER_ADMIN)) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
//        }
//
//        logger.info { "Processing upload completion for tenant $tenantName, record $recordId" }
//        try {
//            recordService.completeUpload(tenantName, recordId)
//            logger.info { "Upload completed successfully for record $recordId" }
//            return ResponseEntity.ok().build()
//        } catch (e: Exception) {
//            logger.error(e) { "Error completing upload: ${e.message}" }
//            throw e
//        }
//    }

}