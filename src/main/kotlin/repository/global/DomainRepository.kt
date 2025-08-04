package com.muditsahni.repository.global

import com.muditsahni.model.entity.Domain
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface DomainRepository : CoroutineCrudRepository<Domain, String> {
    suspend fun findByNameAndDeletedFalse(name: String): Domain?
    suspend fun existsByNameAndDeletedFalse(name: String): Boolean
}