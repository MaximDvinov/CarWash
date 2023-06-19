package com.carwash.feture.service

import kotlinx.serialization.Serializable

@Serializable
data class ServiceCategory(
    val id: Int? = null,
    val name: String
)

@Serializable
data class Service(
    val id: Int? = null,
    val category: ServiceCategory? = null,
    val name: String,
    val price: Double
)