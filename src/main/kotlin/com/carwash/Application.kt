package com.carwash

import com.carwash.di.appModule
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.carwash.plugins.*
import org.koin.ktor.plugin.Koin

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Koin){
        modules(appModule)
    }

    configureAuth()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRouting()
}
