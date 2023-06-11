package com.carwash.di

import com.carwash.db.configureDatabases
import com.carwash.db.dao.UserDao
import com.carwash.service.UserService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    single { configureDatabases() }

    singleOf(::UserDao)
    singleOf(::UserService)
}