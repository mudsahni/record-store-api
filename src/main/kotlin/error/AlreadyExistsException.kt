package com.muditsahni.error

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
open class AlreadyExistsException(entity: String, type: String, attribute: String)
    : RuntimeException("$entity with $type '$attribute' already exists")