package com.carwash.plugins

import com.carwash.feture.cars.carsRoute
import com.carwash.feture.orders.orderRoute
import com.carwash.feture.service.serviceRoutes
import com.carwash.feture.users.userRoutes
import io.ktor.server.application.*

fun Application.configureRouting() {
    userRoutes()
    serviceRoutes()
    carsRoute()
    orderRoute()
}
