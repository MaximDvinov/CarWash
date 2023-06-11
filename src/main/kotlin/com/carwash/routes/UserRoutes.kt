package com.carwash.routes

import com.carwash.ResultResponse
import com.carwash.models.EmailWithPassword
import com.carwash.models.User
import com.carwash.plugins.UserPrincipal
import com.carwash.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import org.koin.ktor.ext.inject

fun Application.userRoutes() {
    val userService by inject<UserService>()

    routing {
        get("/login") {
            val emailWithPassword = call.receive<EmailWithPassword>()

            if (emailWithPassword.password.isBlank()) call.respond(
                HttpStatusCode.BadRequest,
                ResultResponse.Error("Invalid email or password")
            )

            val token = userService.authUser(emailWithPassword)

            if (token != null) {
                call.respond(HttpStatusCode.OK, token)
            } else {
                call.respond(HttpStatusCode.BadRequest, ResultResponse.Error("Invalid email or password"))
            }
        }

        authenticate("auth-bearer") {
            route("/users") {
                get("/") {
                    isAdministrator()

                    val users = userService.getAllUser()
                    call.respond(HttpStatusCode.OK, users)
                }

                get("/{id}") {
                    val user = userService.getUserById(call.parameters["id"]?.toIntOrNull()) ?: call.respond(
                        status = HttpStatusCode.NotFound,
                        ResultResponse.Error("User not found"),
                    )

                    call.respond(HttpStatusCode.OK, user)
                }

                post {
                    isAdministrator()

                    val user = call.receive<User>()
                    val newUser = userService.createUser(user)

                    if (newUser != null) {
                        call.respond(HttpStatusCode.OK, newUser)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ResultResponse.Error("User not created"))
                    }
                }

                delete("/{id}") {
                    isAdministrator()
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id != null) {
                        val userId = userService.deleteUser(id)

                        call.respond(HttpStatusCode.OK, ResultResponse.Success(userId))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ResultResponse.Error("User not deleted"))
                    }
                }

                put("/{id}") {
                    isAdministrator()

                    val id = call.parameters["id"]?.toIntOrNull()
                    val user = call.receive<User>()

                    if (id != null) {
                        val updatedUser = userService.updateUser(id, user)

                    }
                }

                patch("/role") {
                    isAdministrator()
                    val userId = call.request.queryParameters["id"]?.toIntOrNull()
                    val roleId = call.request.queryParameters["role"]?.toIntOrNull()

                    if (userId != null && roleId != null) {
                        userService.updateRole(userId, roleId)
                        call.respond(HttpStatusCode.OK, ResultResponse.Success("Role updated"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ResultResponse.Error("Invalid parameters"))
                    }
                }
            }
        }
    }
}

suspend fun PipelineContext<Unit, ApplicationCall>.isAdministrator() {
    if (call.principal<UserPrincipal>()?.user?.role?.id != 1) call.respond(
        HttpStatusCode.Forbidden,
        ResultResponse.Error("You are not administrator")
    )
}