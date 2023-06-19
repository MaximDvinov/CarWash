package com.carwash.feture.orders

import com.carwash.feture.users.isAdministrator
import com.carwash.plugins.UserPrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.orderRoute() {
    val orderRepository by inject<OrderRepository>()

    routing {
        authenticate("auth-bearer") {
            get("/orders/admin") {
                isAdministrator()

                val page = call.request.queryParameters["page"]?.toLongOrNull()
                val size = call.request.queryParameters["size"]?.toIntOrNull()
                val sortField = call.request.queryParameters["sortField"]
                val sortOrder = call.request.queryParameters["sortOrder"]
                val operatorStartDate = call.request.queryParameters["operator_start_date"]
                val operatorEndDate = call.request.queryParameters["operator_end_date"]

                val service_id = call.request.queryParameters["service_id"]
                val customerCarId = call.request.queryParameters["customer_car_id"]
                val employeeId = call.request.queryParameters["employee_id"]
                val status = call.request.queryParameters["status"]
                val administratorId = call.request.queryParameters["administrator_id"]
                val customerId = call.request.queryParameters["customer_id"]


                call.respond(
                    orderRepository.getALlOrders(
                        page = page, size = size, sortField = sortField, sortOrder = sortOrder, filter = mapOf(
                            "service" to service_id,
                            "customerCar" to customerCarId,
                            "employee" to employeeId,
                            "status" to status,
                            "administrator" to administratorId,
                            "customer" to customerId
                        ),
                        operatorStartDate,
                        operatorEndDate
                    )
                )
            }

            get("/orders") {
                val user = call.principal<UserPrincipal>()?.user

                val page = call.request.queryParameters["page"]?.toLongOrNull()
                val size = call.request.queryParameters["size"]?.toIntOrNull()
                val sortField = call.request.queryParameters["sortField"]
                val sortOrder = call.request.queryParameters["sortOrder"]
                val operatorStartDate = call.request.queryParameters["operator_start_date"]
                val operatorEndDate = call.request.queryParameters["operator_end_date"]

                val service_id = call.request.queryParameters["service_id"]
                val customerCarId = call.request.queryParameters["customer_car_id"]
                val status = call.request.queryParameters["status"]

                if (user?.role?.id == 1) {
                    call.respond(
                        orderRepository.getALlOrders(
                            page = page, size = size, sortField = sortField, sortOrder = sortOrder, filter = mapOf(
                                "service" to service_id,
                                "customerCar" to customerCarId,
                                "status" to status,
                            ),
                            operatorStartDate,
                            operatorEndDate
                        )
                    )
                }

                if (user?.role?.id == 2) {
                    call.respond(
                        orderRepository.getALlOrders(
                            page = page, size = size, sortField = sortField, sortOrder = sortOrder, filter = mapOf(
                                "service" to service_id,
                                "customerCar" to customerCarId,
                                "employee" to user.id.toString(),
                                "status" to status,
                            ),
                            operatorStartDate,
                            operatorEndDate
                        )
                    )
                }

                if (user?.role?.id == 3) {
                    call.respond(
                        orderRepository.getALlOrders(
                            page = page, size = size, sortField = sortField, sortOrder = sortOrder, filter = mapOf(
                                "service" to service_id,
                                "customerCar" to customerCarId,
                                "customer" to user.id.toString(),
                                "status" to status,
                            ),
                            operatorStartDate,
                            operatorEndDate
                        )
                    )
                }
            }

            post("/orders") {
                isAdministrator()

                val user = call.principal<UserPrincipal>()?.user
                val order = call.receive<OrderRequest>()
                orderRepository.createOrder(order, user?.id)
            }

            delete("/orders/{id}") {
                isAdministrator()

                val id = call.request.queryParameters["id"]?.toIntOrNull()
                if (id != null) {
                    val deletedCar = orderRepository.deleteOrder(id)
                    if (deletedCar) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            put("/orders/{id}") {
                isAdministrator()

                val id = call.request.queryParameters["id"]?.toIntOrNull()

                if (id != null) {
                    val order = call.receive<OrderRequest>()
                    val updated = orderRepository.updateOrder(id, order)
                    if (updated) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }

    }
}