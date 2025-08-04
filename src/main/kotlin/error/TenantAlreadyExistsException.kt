package com.muditsahni.error

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class TenantAlreadyExistsException(name: String)
    : AlreadyExistsException("Tenant", "name", name)