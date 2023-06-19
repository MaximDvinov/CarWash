package com.carwash.feture.orders

import com.carwash.db.dbQuery
import com.carwash.db.getDate
import com.carwash.db.getOrderByString
import com.carwash.feture.cars.*
import com.carwash.feture.service.Service
import com.carwash.feture.service.ServiceCategoriesTable
import com.carwash.feture.service.ServiceCategory
import com.carwash.feture.service.ServicesTable
import com.carwash.feture.users.UserDao
import com.carwash.feture.users.UsersTable
import com.carwash.feture.users.models.toSimple
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter

object OrdersTable : IntIdTable() {
    val serviceId = reference("service_id", ServicesTable, onDelete = ReferenceOption.CASCADE)
    val customerCarId = reference("customer_car_id", CustomerCarsTable, onDelete = ReferenceOption.CASCADE)
    val employeeId = reference("employee_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val status = integer("status")
    val startDate = date("start_date")
    val endDate = date("end_date")
    val administratorId = reference("administrator_id", UsersTable, onDelete = ReferenceOption.CASCADE)
}

//class OrderEntity(id: EntityID<Int>) : IntEntity(id) {
//    companion object : IntEntityClass<OrderEntity>(OrdersTable)
//
//    var serviceId by ServicesTable referencedOn OrdersTable.serviceId
//    var customerCarId by OrdersTable.customerCarId
//    var employeeId by OrdersTable.employeeId
//    var status by OrdersTable.status
//    var startDate by OrdersTable.startDate
//    var endDate by OrdersTable.endDate
//    var administratorId by OrdersTable.administratorId
//}

class OrdersDao(database: Database, val dao: UserDao) {
    init {
        transaction(database) {
            SchemaUtils.create(OrdersTable)
        }
    }

    val employee = UsersTable.alias("employee")
    val admin = UsersTable.alias("admin")
    val customer = UsersTable.alias("customer")

    suspend fun createOrder(order: Order): Order? = dbQuery {
        if (order.service.id != null && order.customerCar.id != null && order.employee.id != null && order.administrator.id != null) {
            try {
                val generatedId = OrdersTable.insertAndGetId { row ->
                    row[serviceId] = EntityID(order.service.id, ServicesTable)
                    row[customerCarId] = EntityID(order.customerCar.id, CustomerCarsTable)
                    row[employeeId] = EntityID(order.employee.id, UsersTable)
                    row[status] = order.status.ordinal
                    row[startDate] = order.startDate.toJavaLocalDate()
                    row[endDate] = order.endDate.toJavaLocalDate()
                    row[administratorId] = EntityID(order.administrator.id, UsersTable)
                }
                order.copy(id = generatedId.value)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    suspend fun getAllOrders(
        page: Long? = null,
        size: Int? = null,
        filter: Map<String, String>? = null,
        sortField: String? = null,
        sortOrder: String? = null,
        operatorStartDate: String? = null,
        operatorEndDate: String?
    ) = dbQuery {
        val query = OrdersTable
            .join(
                employee,
                JoinType.LEFT,
                onColumn = OrdersTable.employeeId,
                otherColumn = employee[UsersTable.id]
            )
            .join(
                admin,
                JoinType.LEFT,
                onColumn = OrdersTable.administratorId,
                otherColumn = admin[UsersTable.id]
            )
            .join(ServicesTable, JoinType.LEFT, onColumn = OrdersTable.serviceId, otherColumn = ServicesTable.id)
            .join(
                ServiceCategoriesTable,
                JoinType.LEFT,
                onColumn = ServicesTable.serviceCategoryId,
                otherColumn = ServiceCategoriesTable.id
            )
            .join(
                CustomerCarsTable,
                JoinType.LEFT,
                onColumn = OrdersTable.customerCarId,
                otherColumn = CustomerCarsTable.id
            )
            .join(
                CarsTable,
                JoinType.LEFT,
                onColumn = CustomerCarsTable.carId,
                otherColumn = CarsTable.id
            )
            .join(
                BrandTable,
                JoinType.LEFT,
                onColumn = CarsTable.brandId,
                otherColumn = BrandTable.id
            )
            .join(
                customer,
                JoinType.LEFT,
                onColumn = CustomerCarsTable.customerId,
                otherColumn = customer[UsersTable.id]
            )
            .selectAll()

        filter?.get("service")?.toIntOrNull()?.let {
            query.andWhere { OrdersTable.serviceId eq it }
        }

        filter?.get("customerCar")?.toIntOrNull()?.let {
            query.andWhere { OrdersTable.customerCarId eq it }
        }

        filter?.get("employee")?.toIntOrNull()?.let {
            query.andWhere { OrdersTable.employeeId eq it }
        }

        filter?.get("status")?.toIntOrNull()?.let {
            query.andWhere { OrdersTable.status eq it }
        }

        filter?.get("administrator")?.toIntOrNull()?.let {
            query.andWhere { OrdersTable.administratorId eq it }
        }

        filter?.get("customer")?.toIntOrNull()?.let {
            query.andWhere { customer[UsersTable.id] eq it }
        }

        filter?.get("startDate")?.getDate()?.let {
            when (operatorStartDate) {
                "lessEq" -> query.andWhere { OrdersTable.startDate lessEq it }
                "greaterEq" -> query.andWhere { OrdersTable.startDate greaterEq it }
                "less" -> query.andWhere { OrdersTable.startDate less it }
                "greater" -> query.andWhere { OrdersTable.startDate greater it }
                else -> {
                    query.andWhere { OrdersTable.startDate eq it }
                }
            }
        }

        filter?.get("endDate")?.getDate()?.let {
            when (operatorEndDate) {
                "lessEq" -> query.andWhere { OrdersTable.endDate lessEq it }
                "greaterEq" -> query.andWhere { OrdersTable.endDate greaterEq it }
                "less" -> query.andWhere { OrdersTable.endDate less it }
                "greater" -> query.andWhere { OrdersTable.endDate greater it }
                else -> {
                    query.andWhere { OrdersTable.endDate eq it }
                }
            }
        }

        val order = getOrderByString(sortOrder)

        query.orderBy(
            when (sortField) {
                "startDate" -> OrdersTable.startDate to order
                "endDate" -> OrdersTable.endDate to order
                "status" -> OrdersTable.status to order
                else -> {
                    OrdersTable.id to order
                }
            }
        )


        if (page != null && size != null) {
            query.limit(size, (page - 1) * size)
        }

        query.map { tableToOrder(it).toSimple() }
    }

    suspend fun updateOrder(order: OrderRequest, id: Int) = dbQuery {
        try {
            OrdersTable.update({ OrdersTable.id eq id }) { row ->
                if (order.serviceId != null) row[serviceId] = EntityID(order.serviceId, ServicesTable)
                if (order.customerCarId != null) row[customerCarId] = EntityID(order.customerCarId, CustomerCarsTable)
                if (order.employeeId != null) row[employeeId] = EntityID(order.employeeId, UsersTable)
                if (order.status != null) row[status] = order.status.ordinal
                if (order.startDate != null) row[startDate] = order.startDate.toJavaLocalDate()
                if (order.endDate != null) row[endDate] = order.endDate.toJavaLocalDate()
                if (order.administratorId != null) row[administratorId] = EntityID(order.administratorId, UsersTable)
            }
        } catch (_: Exception) {
            0
        }
    }

    suspend fun deleteOrder(order: Int) = dbQuery {
        try {
            OrdersTable.deleteWhere { OrdersTable.id eq order }
        } catch (_: Exception) {
            0
        }
    }


    suspend fun tableToOrder(
        it: ResultRow,
    ) = Order(
        id = it[OrdersTable.id].value,
        service = tableToServiceData(it),
        customerCar = tableToCustomerCar(it),
        employee = dao.tableToUserData(employee, it),
        status = if (it[OrdersTable.status] == 0) OrderStatus.IN_PROGRESS else OrderStatus.COMPLETED,
        startDate = it[OrdersTable.startDate].toKotlinLocalDate(),
        endDate = it[OrdersTable.endDate].toKotlinLocalDate(),
        administrator = dao.tableToUserData(admin, row = it)
    )

    private fun tableToServiceData(it: ResultRow): Service {
        return Service(
            id = it[ServicesTable.id].value,
            category = tableToCategoryData(it),
            name = it[ServicesTable.name],
            price = it[ServicesTable.price]
        )
    }

    private fun tableToCategoryData(it: ResultRow): ServiceCategory {
        return ServiceCategory(
            id = it[ServiceCategoriesTable.id].value, name = it[ServiceCategoriesTable.name]
        )
    }

    private suspend fun tableToCustomerCar(it: ResultRow): CustomerCar {
        return CustomerCar(
            id = it[CustomerCarsTable.id].value,
            year = it[CustomerCarsTable.year],
            number = it[CustomerCarsTable.number],
            image = it[CustomerCarsTable.image],
            car = tableToCar(it),
            customer = dao.tableToUserData(employee, row = it),
        )
    }

    private suspend fun tableToCar(it: ResultRow): Car {
        return Car(
            id = it[CarsTable.id].value,
            name = it[CarsTable.model],
            brand = tableToBrand(it)
        )
    }

    private suspend fun tableToBrand(it: ResultRow): Brand {
        return Brand(
            id = it[BrandTable.id].value,
            name = it[BrandTable.name]
        )
    }


}

fun Order.toSimple(): SimpleOrder {
    return SimpleOrder(
        id = this.id,
        service = this.service,
        customerCar = this.customerCar.toSimple(),
        employee = this.employee.toSimple(),
        status = this.status,
        startDate = this.startDate,
        endDate = this.endDate,
        administrator = this.administrator.toSimple()
    )

}
