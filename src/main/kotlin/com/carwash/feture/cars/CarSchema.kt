package com.carwash.feture.cars

import com.carwash.db.dbQuery
import com.carwash.db.getOrderByString
import com.carwash.db.upsert
import com.carwash.feture.service.ServicesTable
import com.carwash.feture.users.UserDao
import com.carwash.feture.users.UsersTable
import com.carwash.feture.users.models.User
import com.carwash.feture.users.models.toResponse
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CarsTable : IntIdTable() {
    val brandId = reference(name = "brand_id", foreign = BrandTable, onDelete = ReferenceOption.CASCADE)
    val model = varchar("model", 255)
}

object BrandTable : IntIdTable() {
    val name = varchar("name", 255)
}

object CustomerCarsTable : IntIdTable() {
    val carId = reference(name = "car_id", foreign = CarsTable, onDelete = ReferenceOption.CASCADE)
    val customerId = reference(name = "customer_id", foreign = UsersTable, onDelete = ReferenceOption.CASCADE)
    val year = integer("year")
    val number = varchar("number", 255)
    val image = varchar("image", 255).nullable()
}

class CarsDao(database: Database, val userDao: UserDao) {
    init {
        transaction(database) {
            SchemaUtils.create(CarsTable, BrandTable, CustomerCarsTable)
        }
    }

    suspend fun createCar(car: Car) = dbQuery {
        var newBrandId: Int? = null
        if (car.brand?.id != null) {
            if (upsertBrand(car.brand.id, car.brand)) {
                newBrandId = car.brand.id
            }
        }

        val newCarId = CarsTable.insertAndGetId {
            it[model] = car.name
            try {
                if (newBrandId != null) it[brandId] = EntityID(newBrandId, BrandTable)
            } catch (_: Exception) {
            }
        }

        car.copy(id = newCarId.value)
    }

    suspend fun getAllCars(
        page: Long? = null,
        size: Int? = null,
        filter: Map<String, String>? = null,
        sortField: String? = null,
        sortOrder: String? = null
    ) = dbQuery {
        val query = (CarsTable leftJoin BrandTable leftJoin UsersTable).selectAll()

        filter?.get("model")?.let {
            query.andWhere { CarsTable.model like "%${it}%" }
        }

        filter?.get("brand")?.let {
            query.andWhere { BrandTable.name like "%${it}%" }
        }

        val order = getOrderByString(sortOrder)

        query.orderBy(
            when (sortField) {
                "model" -> ServicesTable.name to order
                "brand" -> BrandTable.name to order
                else -> {
                    ServicesTable.id to order
                }
            }
        )

        if (page != null && size != null) {
            query.limit(size, (page - 1) * size)
        }

        query.map { tableToCar(it) }
    }

    suspend fun updateCar(id: Int, car: Car) = dbQuery {
        var newBrandId: Int? = null
        if (car.brand?.id != null) {
            if (upsertBrand(car.brand.id, car.brand)) {
                newBrandId = car.brand.id
            }
        }

        CarsTable.update({ CarsTable.id eq id }) {
            it[model] = car.name
            try {
                if (newBrandId != null) it[brandId] = EntityID(newBrandId, BrandTable)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun getCarById(id: Int) = dbQuery {
        (CarsTable leftJoin BrandTable).select { CarsTable.id eq id }.singleOrNull()?.let {
            tableToCar(it)
        }
    }

    suspend fun getBrandById(id: Int) = dbQuery {
        BrandTable.select { BrandTable.id eq id }.singleOrNull()?.let {
            tableToBrand(it)
        }
    }

    suspend fun getAllBrands(
        page: Long? = null,
        size: Int? = null,
        filter: Map<String, String>? = null,
        sortField: String? = null,
        sortOrder: String? = null
    ) = dbQuery {
        val query = (BrandTable).selectAll()

        filter?.get("name")?.let {
            query.andWhere { BrandTable.name like "%${it}%" }
        }

        val order = getOrderByString(sortOrder)

        query.orderBy(
            when (sortField) {
                "name" -> BrandTable.name to order
                else -> {
                    BrandTable.id to order
                }
            }
        )

        if (page != null && size != null) {
            query.limit(size, (page - 1) * size)
        }

        query.map { tableToBrand(it) }
    }

    suspend fun deleteCar(id: Int) = dbQuery {
        CarsTable.deleteWhere { CarsTable.id eq id }
    }

    suspend fun createBrand(brand: Brand) = dbQuery {
        val newId = BrandTable.insertAndGetId {
            it[name] = brand.name
        }
        brand.copy(id = newId.value)
    }

    suspend fun updateBrand(id: Int, brand: Brand) = dbQuery {
        BrandTable.update({ BrandTable.id eq id }) {
            it[name] = brand.name
        }
    }

    suspend fun upsertBrand(brandId: Int, brand: Brand) = dbQuery {
        BrandTable.upsert {
            it[name] = brand.name
            it[id] = brandId
        }.insertedCount > 0
    }

    suspend fun deleteBrand(id: Int) = dbQuery {
        BrandTable.deleteWhere { BrandTable.id eq id }
    }

    suspend fun createCustomerCar(customerCar: CustomerCar, user: User, car: Car) = dbQuery {
        if (car.id == null || user.id == null) {
            return@dbQuery null
        }
        val newId = CustomerCarsTable.insertAndGetId {
            it[carId] = EntityID(car.id, CarsTable)
            it[customerId] = EntityID(user.id, UsersTable)
            it[year] = customerCar.year ?: 0
            it[number] = customerCar.number ?: ""
            it[image] = customerCar.image
        }

        customerCar.copy(
            id = newId.value,
            car = car,
            customer = user
        ).toSimple()
    }

    suspend fun updateCustomerCar(id: Int, customer: CustomerCar) = dbQuery {
        CustomerCarsTable.update({ CustomerCarsTable.id eq id }) {
            if (customer.year != null) it[year] = customer.year
            if (customer.number != null) it[number] = customer.number
            if (customer.image != null) it[image] = customer.image
        }
    }

    suspend fun deleteCustomerCar(id: Int) = dbQuery {
        CustomerCarsTable.deleteWhere { CustomerCarsTable.id eq id }
    }

    suspend fun getCustomerCarById(id: Int) = dbQuery {
        (CustomerCarsTable leftJoin CarsTable leftJoin UsersTable).select { CustomerCarsTable.id eq id }.singleOrNull()
            ?.let {
                tableToCustomerCar(it)
            }
    }

    suspend fun getAllCustomerCars(
        page: Long? = null,
        size: Int? = null,
        filter: Map<String, String>? = null,
        sortField: String? = null,
        sortOrder: String? = null,
        operator: String? = null
    ) = dbQuery {
        val query = (CustomerCarsTable leftJoin CarsTable leftJoin BrandTable leftJoin UsersTable).selectAll()

        filter?.get("customer")?.toIntOrNull()?.let {
            query.andWhere { UsersTable.id eq it }
        }

        filter?.get("car")?.toIntOrNull()?.let {
            query.andWhere { CarsTable.id eq it }
        }

        filter?.get("year")?.toIntOrNull()?.let {
            when (operator) {
                "lessEq" -> query.andWhere { CustomerCarsTable.year lessEq it }
                "greaterEq" -> query.andWhere { CustomerCarsTable.year greaterEq it }
                "less" -> query.andWhere { CustomerCarsTable.year less it }
                "greater" -> query.andWhere { CustomerCarsTable.year greater it }
                else -> {
                    query.andWhere { CustomerCarsTable.year eq it }
                }
            }
        }

        filter?.get("number")?.let {
            query.andWhere { CustomerCarsTable.number like "%${it}%" }
        }

        val order = getOrderByString(sortOrder)

        query.orderBy(
            when (sortField) {
                "year" -> CustomerCarsTable.year to order
                else -> {
                    CustomerCarsTable.id to order
                }
            }
        )

        if (page != null && size != null) {
            query.limit(size, (page - 1) * size)
        }

        query.map { tableToCustomerCar(it).toSimple() }
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

    private suspend fun tableToCustomerCar(it: ResultRow): CustomerCar {
        return CustomerCar(
            id = it[CustomerCarsTable.id].value,
            year = it[CustomerCarsTable.year],
            number = it[CustomerCarsTable.number],
            image = it[CustomerCarsTable.image],
            car = tableToCar(it),
            customer = userDao.tableToUserData(row = it),
        )
    }


}
