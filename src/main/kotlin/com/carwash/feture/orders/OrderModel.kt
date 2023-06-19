package com.carwash.feture.orders

import com.carwash.feture.cars.CustomerCar
import com.carwash.feture.cars.CustomerCarSimple
import com.carwash.feture.service.Service
import com.carwash.feture.users.models.User
import com.carwash.feture.users.models.UserResponse
import com.carwash.feture.users.models.UserSimpleInfo
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val id: Int? = null,
    val serviceId: Int? = null,
    val customerCarId: Int? = null,
    val employeeId: Int? = null,
    val status: OrderStatus? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val administratorId: Int? = null
)

@Serializable
enum class OrderStatus { IN_PROGRESS, COMPLETED }

@Serializable
data class Order(
    val id: Int? = null,
    val service: Service,
    val customerCar: CustomerCar,
    val employee: User,
    val status: OrderStatus,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val administrator: User
)


@Serializable
data class SimpleOrder(
    val id: Int? = null,
    val service: Service,
    val customerCar: CustomerCarSimple,
    val employee: UserSimpleInfo,
    val status: OrderStatus,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val administrator: UserSimpleInfo
)
