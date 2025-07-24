package com.muditsahni.model.entity

enum class Role {
    USER,
    ADMIN,
    SUPER_ADMIN;

    companion object {
        fun fromString(role: String): Role {
            return Role.entries.find { it.name.equals(role, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown role: $role")
        }
    }
}