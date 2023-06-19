package com.carwash.feture.cars

import com.carwash.ResultResponse
import com.carwash.feture.users.isAdministrator
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.io.File

fun Application.carsRoute() {
    val carRepository by inject<CarsRepository>()

    routing {
        get("/uploads/{filename}") {
            val filename = call.parameters["filename"]

            if (filename != null) {
                call.respondFile(File("uploads/$filename"))
            }
        }

        authenticate("auth-bearer") {
            carsRoute(carRepository)
            brandRoute(carRepository)
            customerCar(carRepository)

            patch("/customer-cars/{id}/image") {
                isAdministrator()

                val id = call.parameters["id"]?.toIntOrNull()
                if (id != null) {
                    val image = call.receiveMultipart()
                    val added = carRepository.addImage(image, id)
                    if (added) {
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

private fun Route.customerCar(carRepository: CarsRepository) {
    route("/customer-cars") {
        get {
            isAdministrator()

            val page = call.request.queryParameters["page"]?.toLongOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val sortField = call.request.queryParameters["sortField"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val year = call.request.queryParameters["year"]
            val number = call.request.queryParameters["number"]
            val operator = call.request.queryParameters["operator"]

            call.respond(
                HttpStatusCode.OK, carRepository.getAllCustomerCars(
                    page = page, size = size, sortField = sortField, sortOrder = sortOrder, filter = mapOf(
                        "year" to year, "number" to number
                    ), operator = operator
                )
            )
        }

        post {
            isAdministrator()

            val userId = call.request.queryParameters["user_id"]?.toIntOrNull()
            val carId = call.request.queryParameters["car_id"]?.toIntOrNull()

            val car = call.receive<CustomerCar>()

            if (userId != null && carId != null) {

                when (val createdCar = carRepository.createCustomerCar(userId, carId, car)) {
                    is ResultResponse.Error -> {
                        call.respond(HttpStatusCode.BadRequest, createdCar)
                    }

                    is ResultResponse.Success<CustomerCarSimple> -> {
                        call.respond(HttpStatusCode.OK, createdCar.data)
                    }

                    else -> {}
                }
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        put {
            isAdministrator()
            val id = call.request.queryParameters["id"]?.toIntOrNull()
            if (id != null) {
                val customerCar = call.receive<CustomerCar>()
                val updatedCar = carRepository.updateCustomerCar(id, customerCar)
                if (updatedCar) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        delete {
            isAdministrator()

            val id = call.request.queryParameters["id"]?.toIntOrNull()
            if (id != null) {
                val deletedCar = carRepository.deleteCustomerCar(id)
                if (deletedCar) {
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

private fun Route.brandRoute(carRepository: CarsRepository) {
    route("/brands") {
        get {
            isAdministrator()

            val page = call.request.queryParameters["page"]?.toLongOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val sortField = call.request.queryParameters["sortField"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val name = call.request.queryParameters["name"]

            call.respond(
                HttpStatusCode.OK, carRepository.getAllBrands(
                    page = page, size = size, sortField = sortField, sortOrder = sortOrder, filter = mapOf(
                        "name" to name
                    )
                )
            )
        }

        post {
            isAdministrator()

            val brand = call.receive<Brand>()

            val createdBrand = carRepository.createBrand(brand)
            call.respond(HttpStatusCode.OK, createdBrand)
        }

        delete("/{id}") {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                if (carRepository.deleteBrand(id)) {
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
                call.respond(HttpStatusCode.BadRequest)
            } else {
                val brand = call.receive<Brand>()
                if (carRepository.updateBrand(id, brand)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

    }
}

private fun Route.carsRoute(carRepository: CarsRepository) {
    route("/cars") {
        get {
            isAdministrator()

            val page = call.request.queryParameters["page"]?.toLongOrNull()
            val size = call.request.queryParameters["size"]?.toIntOrNull()
            val sortField = call.request.queryParameters["sortField"]
            val sortOrder = call.request.queryParameters["sortOrder"]

            val model = call.request.queryParameters["model"]
            val brand = call.request.queryParameters["brand"]

            call.respond(
                HttpStatusCode.OK, carRepository.getAllCars(
                    page = page, size = size, sortField = sortField, sortOrder = sortOrder, filter = mapOf(
                        "model" to model, "brand" to brand
                    )
                )
            )
        }

        get("/id") {
            isAdministrator()

            val id = call.request.queryParameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                val car = carRepository.getCarById(id.toInt())
                if (car == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(car)
                }
            }
        }

        post {
            isAdministrator()

            val car = call.receive<Car>()
            val createdCar = carRepository.createCar(car)
            call.respond(HttpStatusCode.OK, createdCar)
        }

        delete("/{id}") {
            isAdministrator()

            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                if (carRepository.deleteCar(id)) {
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
                call.respond(HttpStatusCode.BadRequest)
            } else {
                val car = call.receive<Car>()
                if (carRepository.updateCar(id, car)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}

