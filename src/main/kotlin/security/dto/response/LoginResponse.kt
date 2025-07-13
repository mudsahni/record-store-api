package com.muditsahni.security.dto.response

data class LoginResponse(
    val token: String? = null,
    val refreshToken: String? = null,
    val user: UserDto? = null,
    val tenant: TenantDto? = null,
    val mustChangePassword: Boolean = false,
    val error: String? = null
)
