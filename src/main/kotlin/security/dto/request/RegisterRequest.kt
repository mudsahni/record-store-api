package com.muditsahni.security.dto.request

data class RegisterRequest(
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val phoneNumber: String,
    val tenantName: String,
    val password: String
)
