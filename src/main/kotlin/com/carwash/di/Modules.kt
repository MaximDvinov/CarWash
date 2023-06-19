package com.carwash.di

import com.carwash.db.configureDatabases
import com.carwash.feture.users.*
import com.carwash.feture.service.*
import com.carwash.feture.cars.*
import com.carwash.feture.orders.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    single { configureDatabases() }

    singleOf(::UserDao)
    singleOf(::UserRepository)

    singleOf(::ServiceDao)
    singleOf(::ServiceRepository)

    singleOf(::CarsDao)
    singleOf(::CarsRepository)

    singleOf(::OrdersDao)
    singleOf(::OrderRepository)
}