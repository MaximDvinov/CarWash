package com.carwash.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun configureDatabases(): Database {
    return Database.connect(
        url = "jdbc:postgresql://localhost:5432/car_wash",
        user = "postgres",
        driver = "org.postgresql.Driver",
        password = "postgres"
    )
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
