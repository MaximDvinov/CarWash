package com.carwash.feture.cars

import com.carwash.feture.users.models.User
import com.carwash.feture.users.models.UserResponse
import com.carwash.feture.users.models.UserSimpleInfo
import com.carwash.feture.users.models.toSimple
import kotlinx.serialization.Serializable

@Serializable
data class Brand(
    val id: Int? = null,
    val name: String
)

@Serializable
data class Car(
    val id: Int? = null,
    val name: String,
    val brand: Brand? = null
)

@Serializable
data class CustomerCar(
    val id: Int? = null,
    val car: Car? = null,
    val customer: User? = null,
    val year: Int? = null,
    val number: String? = null,
    val image: String? = null
)

@Serializable
data class CustomerCarSimple(
    val id: Int? = null,
    val car: Car? = null,
    val customer: UserSimpleInfo? = null,
    val year: Int,
    val number: String,
    val image: String? = null
)

fun CustomerCar.toSimple(): CustomerCarSimple {
    return CustomerCarSimple(
        id = this.id,
        car = this.car,
        customer = this.customer?.toSimple(),
        year = this.year ?: 0,
        number = this.number ?: "",
        image = this.image
    )
}