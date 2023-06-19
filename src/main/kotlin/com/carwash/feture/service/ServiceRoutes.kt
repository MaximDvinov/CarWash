package com.carwash.feture.service

import com.carwash.ResultResponse
import com.carwash.feture.users.isAdministrator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Application.serviceRoutes() {
    val serviceRepository by inject<ServiceRepository>()

    routing {
        authenticate("auth-bearer") {
            servicesRoute(serviceRepository)

            categoryServicesRoute(serviceRepository)
        }
    }
}

private fun Route.servicesRoute(serviceRepository: ServiceRepository) {
    route("/services") {
        get {
            isAdministrator()

            val page = call.request.queryParameters["page"]?.toLongOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val sortField = call.request.queryParameters["sortField"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val name = call.request.queryParameters["name"]
            val price = call.request.queryParameters["price"]
            val categoryName = call.request.queryParameters["category"]
            val operator = call.request.queryParameters["operator"]

            if (price == null && operator != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ResultResponse.Error<String>("The \"operator\" parameter can only be used with the \"price\" parameter")
                )
            }

            call.respond(
                HttpStatusCode.OK,
                serviceRepository.getAllService(
                    page = page,
                    size = size,
                    sortField = sortField,
                    sortOrder = sortOrder,
                    filter = mapOf(
                        "name" to name,
                        "price" to price,
                        "category" to categoryName
                    ),
                    operator = operator
                )
            )
        }

        get("/{id}") {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val service = serviceRepository.getServiceById(id)
                if (service != null) {
                    call.respond(HttpStatusCode.OK, service)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        post {
            isAdministrator()

            val service = call.receive<Service>()
            val createdService = serviceRepository.createService(service)

            call.respond(HttpStatusCode.OK, createdService)
        }

        delete("/{id}") {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                if (serviceRepository.deleteService(id)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        put("/{id}") {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val service = call.receive<Service>()
                if (serviceRepository.updateService(id, service)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }


    }
}

private fun Route.categoryServicesRoute(serviceRepository: ServiceRepository) {
    route("/categories") {
        get {
            isAdministrator()

            val page = call.request.queryParameters["page"]?.toLongOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val sortField = call.request.queryParameters["sortField"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val name = call.request.queryParameters["name"]

            serviceRepository.getAllCategory(
                page = page,
                size = size,
                sortField = sortField,
                sortOrder = sortOrder,
                filter = mapOf(
                    "name" to name
                )
            )
        }

        get("/{id}") {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val category = serviceRepository.getCategoryById(id)
                if (category != null) {
                    call.respond(HttpStatusCode.OK, category)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        post {
            isAdministrator()

            val category = call.receive<ServiceCategory>()
            val createdCategory = serviceRepository.createCategory(category)
            call.respond(HttpStatusCode.OK, createdCategory)
        }

        delete {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                if (serviceRepository.deleteCategory(id)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        put {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val category = call.receive<ServiceCategory>()
                if (serviceRepository.updateCategory(id, category)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}