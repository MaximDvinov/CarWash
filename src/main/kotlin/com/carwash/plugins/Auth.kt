package com.carwash.plugins

import com.carwash.feture.users.models.UserResponse
import com.carwash.feture.users.UserRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

data class UserPrincipal(val user: UserResponse) : Principal

fun Application.configureAuth() {
    val userRepository by inject<UserRepository>()

    install(Authentication) {
        bearer("auth-bearer") {
            realm = "Access to the '/' path"

            authenticate { tokenCredential ->
                val userWithToken = userRepository.validateToken(tokenCredential.token)
                if (userWithToken != null) {
                    UserPrincipal(userWithToken.user)
                } else {
                    null
                }
            }
        }
    }
}