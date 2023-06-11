package com.carwash.plugins

import com.carwash.models.UserResponse
import com.carwash.service.UserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

data class UserPrincipal(val user: UserResponse) : Principal

fun Application.configureAuth() {
    val userService by inject<UserService>()

    install(Authentication) {
        bearer("auth-bearer") {
            realm = "Access to the '/' path"

            authenticate { tokenCredential ->
                val userWithToken = userService.validateToken(tokenCredential.token)
                if (userWithToken != null) {
                    UserPrincipal(userWithToken.user)
                } else {
                    null
                }
            }
        }
    }
}