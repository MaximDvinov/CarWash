package com.carwash.plugins

import com.carwash.routes.userRoutes
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*

fun Application.configureRouting() {
    userRoutes()

}
