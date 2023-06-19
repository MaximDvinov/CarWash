package com.carwash

import kotlinx.serialization.Serializable

@Serializable
sealed class ResultResponse<T> {
    @Serializable
    data class Success<T>(val data: T) : ResultResponse<T>()
    @Serializable
    data class Error<T>(val error: String) : ResultResponse<T>()

}