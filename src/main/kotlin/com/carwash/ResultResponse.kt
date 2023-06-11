package com.carwash

import kotlinx.serialization.Serializable

@Serializable
sealed class ResultResponse {
    @Serializable
    data class Success<T>(val message: T) : ResultResponse()
    @Serializable
    data class Error(val error: String) : ResultResponse()

}