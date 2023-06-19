package com.carwash.db

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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

fun getOrderByString(sortOrder: String?) = when (sortOrder) {
    "asc" -> SortOrder.ASC
    "desc" -> SortOrder.DESC
    else -> SortOrder.ASC
}

fun <T : Table> T.upsert(
    where: (SqlExpressionBuilder.() -> Op<Boolean>)? = null,
    vararg keys: Column<*> = (primaryKey ?: throw IllegalArgumentException("primary key is missing")).columns,
    body: T.(InsertStatement<Number>) -> Unit
) = InsertOrUpdate<Number>(this, keys = keys, where = where?.let { SqlExpressionBuilder.it() }).apply {
    body(this)
    execute(TransactionManager.current())
}

class InsertOrUpdate<Key : Any>(
    table: Table,
    isIgnore: Boolean = false,
    private val where: Op<Boolean>? = null,
    private vararg val keys: Column<*>
) : InsertStatement<Key>(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val onConflict = buildOnConflict(table, transaction, where, keys = keys)
        return "${super.prepareSQL(transaction)} $onConflict"
    }
}

fun <T : Table, E> T.batchUpsert(
    data: Collection<E>,
    where: (SqlExpressionBuilder.() -> Op<Boolean>)? = null,
    vararg keys: Column<*> = (primaryKey ?: throw IllegalArgumentException("primary key is missing")).columns,
    body: T.(BatchInsertStatement, E) -> Unit
) = BatchInsertOrUpdate(this, keys = keys, where = where?.let { SqlExpressionBuilder.it() }).apply {
    data.forEach {
        addBatch()
        body(this, it)
    }
    execute(TransactionManager.current())
}

class BatchInsertOrUpdate(
    table: Table,
    isIgnore: Boolean = false,
    private val where: Op<Boolean>? = null,
    private vararg val keys: Column<*>
) : BatchInsertStatement(table, isIgnore) {
    override fun prepareSQL(transaction: Transaction): String {
        val onConflict = buildOnConflict(table, transaction, where, keys = keys)
        return "${super.prepareSQL(transaction)} $onConflict"
    }
}

fun buildOnConflict(
    table: Table,
    transaction: Transaction,
    where: Op<Boolean>? = null,
    vararg keys: Column<*>
): String {
    var updateSetter = (table.columns - keys).joinToString(", ") {
        "${transaction.identity(it)} = EXCLUDED.${transaction.identity(it)}"
    }
    where?.let {
        updateSetter += " WHERE $it"
    }
    return "ON CONFLICT (${keys.joinToString { transaction.identity(it) }}) DO UPDATE SET $updateSetter"
}

fun String?.getDate(): LocalDate? {
    if (this == null) return null

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return LocalDate.parse(this, formatter)
}