package com.carwash.models

import kotlinx.serialization.Serializable


@Serializable
data class TokenWithUser(
    val token: String,
    val user: UserResponse
)

@Serializable
data class Role(val id: Int, val name: String)

@Serializable
data class User(
    val id: Int? = null,
    val firstName: String,
    val lastName: String,
    val patronymic: String?,
    val email: String,
    val isSendNotify: Boolean? = false,
    val roles: Role? = null,
    val password: String = ""
)

@Serializable
data class UserResponse(
    val id: Int? = null,
    val firstName: String,
    val lastName: String,
    val patronymic: String?,
    val email: String,
    val isSendNotify: Boolean? = false,
    val role: Role? = null,
)

fun User.toResponse(): UserResponse {
    return UserResponse(
        id = this.id,
        firstName = this.firstName,
        lastName = this.lastName,
        patronymic = this.patronymic,
        email = this.email,
        isSendNotify = this.isSendNotify,
        role = this.roles
    )
}

@Serializable
data class EmailWithPassword(val email: String, val password: String)
