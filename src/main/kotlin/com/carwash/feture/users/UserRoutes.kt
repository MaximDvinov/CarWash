package com.carwash.feture.users

import com.carwash.ResultResponse
import com.carwash.feture.users.models.EmailWithPassword
import com.carwash.feture.users.models.User
import com.carwash.plugins.UserPrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.koin.ktor.ext.inject

fun Application.userRoutes() {
    val userRepository by inject<UserRepository>()

    routing {
        get("/login") {
            val emailWithPassword = call.receive<EmailWithPassword>()

            if (emailWithPassword.password.isBlank()) call.respond(
                HttpStatusCode.BadRequest,
                ResultResponse.Error<String>("Invalid email or password")
            )

            val token = userRepository.authUser(emailWithPassword)

            if (token != null) {
                call.respond(HttpStatusCode.OK, token)
            } else {
                call.respond(HttpStatusCode.BadRequest, ResultResponse.Error<String>("Invalid email or password"))
            }
        }

        authenticate("auth-bearer") {
            route("/users") {
                get("/") {
                    isAdministrator()

                    val users = userRepository.getAllUser()
                    call.respond(HttpStatusCode.OK, users)
                }

                get("/{id}") {
                    isAdministrator()

                    val user = userRepository.getUserById(call.parameters["id"]?.toIntOrNull()) ?: call.respond(
                        status = HttpStatusCode.NotFound,
                        ResultResponse.Error<String>("User not found"),
                    )

                    call.respond(HttpStatusCode.OK, user)
                }

                post {
                    isAdministrator()

                    val user = call.receive<User>()
                    val newUser = userRepository.createUser(user)

                    if (newUser != null) {
                        call.respond(HttpStatusCode.OK, newUser)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ResultResponse.Error<String>("User not created"))
                    }
                }

                delete("/{id}") {
                    isAdministrator()
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null) {
                        val userId = userRepository.deleteUser(id)

                        call.respond(HttpStatusCode.OK, ResultResponse.Success(userId))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ResultResponse.Error<String>("User not deleted"))
                    }
                }

                put("/{id}") {
                    isAdministrator()

                    val id = call.parameters["id"]?.toIntOrNull()
                    val user = call.receive<User>()

                    if (id != null) {
                        if (userRepository.updateUser(id, user)) {
                            call.respond(HttpStatusCode.OK, ResultResponse.Success("User updated"))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ResultResponse.Error<String>("User not found"))
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ResultResponse.Error<String>("User not updated"))
                    }
                }

                patch("/role") {
                    isAdministrator()
                    val userId = call.request.queryParameters["id"]?.toIntOrNull()
                    val roleId = call.request.queryParameters["role"]?.toIntOrNull()

                    if (userId != null && roleId != null) {
                        userRepository.updateRole(userId, roleId)
                        call.respond(HttpStatusCode.OK, ResultResponse.Success("Role updated"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ResultResponse.Error<String>("Invalid parameters"))
                    }
                }
            }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.isAdministrator() {
    if (call.principal<UserPrincipal>()?.user?.role?.id != 1) call.respond(
        HttpStatusCode.Forbidden,
        ResultResponse.Error<String>("You are not administrator")
    )
}