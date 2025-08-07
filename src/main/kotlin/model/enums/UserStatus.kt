package com.muditsahni.model.enums

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING,
    DELETED;

    companion object {
        fun fromString(value: String): UserStatus {
            return UserStatus.entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown user status: $value")
        }
    }
}