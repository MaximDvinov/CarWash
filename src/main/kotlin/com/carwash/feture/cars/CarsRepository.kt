package com.carwash.feture.cars

import com.carwash.ResultResponse
import com.carwash.feture.users.UserDao
import io.ktor.http.content.*
import java.io.File

class CarsRepository(private val dao: CarsDao, private val userDao: UserDao) {
    suspend fun getAllCars(
        page: Long?,
        size: Int?,
        sortField: String?,
        sortOrder: String?,
        filter: Map<String?, String?>?
    ): List<Car> {
        val newFilter = mutableMapOf<String, String>()

        filter?.forEach { (key, value) ->
            if (key != null && value != null) {
                newFilter[key] = value
            }
        }

        return dao.getAllCars(
            page = page,
            size = size,
            filter = newFilter,
            sortField = sortField,
            sortOrder = sortOrder
        )
    }

    suspend fun getCarById(id: Int): Car? {
        return dao.getCarById(id)
    }

    suspend fun createCar(car: Car): Car {
        return dao.createCar(car)
    }

    suspend fun deleteCar(id: Int): Boolean {
        return dao.deleteCar(id) > 0
    }

    suspend fun updateCar(id: Int, car: Car): Boolean {
        return dao.updateCar(id, car) > 0
    }

    suspend fun getAllBrands(
        page: Long?,
        size: Int?,
        sortField: String?,
        sortOrder: String?,
        filter: Map<String?, String?>?
    ): List<Brand> {
        val newFilter = mutableMapOf<String, String>()

        filter?.forEach { (key, value) ->
            if (key != null && value != null) {
                newFilter[key] = value
            }
        }

        return dao.getAllBrands(
            page = page,
            size = size,
            filter = newFilter,
            sortField = sortField,
            sortOrder = sortOrder
        )

    }

    suspend fun createBrand(brand: Brand): Brand {
        return dao.createBrand(brand)
    }

    suspend fun deleteBrand(id: Int): Boolean {
        return dao.deleteBrand(id) > 0
    }

    suspend fun updateBrand(id: Int, brand: Brand): Boolean {
        return dao.updateBrand(id, brand) > 0
    }

    suspend fun createCustomerCar(
        carId: Int,
        userId: Int,
        customerCar: CustomerCar,
    ): ResultResponse<CustomerCarSimple> {
        val user = userDao.getUserById(userId)
        val car = dao.getCarById(carId)


        if (user == null || car == null) {
            return ResultResponse.Error("User or Car not found")
        }

        val created =
            dao.createCustomerCar(customerCar = customerCar, user = user, car = car)

        return if (created != null) {
            ResultResponse.Success(created)
        } else {
            ResultResponse.Error("Something went wrong")
        }
    }

    suspend fun addImage(image: MultiPartData, id: Int): Boolean {
        var fileDescription = ""
        var fileName = ""

        image.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    fileDescription = part.value
                }

                is PartData.FileItem -> {
                    fileName = part.originalFileName as String
                    val fileBytes = part.streamProvider().readBytes()
                    File("uploads/$fileName").writeBytes(fileBytes)
                }

                else -> {
                    val t = 0
                }
            }
            part.dispose()
        }

        return dao.updateCustomerCar(id, CustomerCar(image = "http://127.0.0.1:8080/uploads/$fileName")) > 0
    }

    suspend fun getAllCustomerCars(
        page: Long?,
        size: Int?,
        sortField: String?,
        sortOrder: String?,
        filter: Map<String?, String?>?,
        operator: String?
    ): List<CustomerCarSimple> {

        val newFilter = mutableMapOf<String, String>()

        filter?.forEach { (key, value) ->
            if (key != null && value != null) {
                newFilter[key] = value
            }
        }

        return dao.getAllCustomerCars(
            page = page,
            size = size,
            filter = newFilter,
            sortField = sortField,
            sortOrder = sortOrder,
            operator = operator
        )
    }

    suspend fun updateCustomerCar(id: Int, customerCar: CustomerCar): Boolean {
        return dao.updateCustomerCar(id, customerCar) > 0
    }

    suspend fun deleteCustomerCar(id: Int): Boolean {
        return dao.deleteCustomerCar(id) > 0
    }

}