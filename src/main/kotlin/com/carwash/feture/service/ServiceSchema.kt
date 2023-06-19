package com.carwash.feture.service

import com.carwash.db.dbQuery
import com.carwash.db.getOrderByString
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object ServiceCategoriesTable : IntIdTable() {
    val name = varchar("name", 255)
}

object ServicesTable : IntIdTable() {
    val serviceCategoryId =
        reference(
            name = "id_service_category",
            foreign = ServiceCategoriesTable,
            onDelete = ReferenceOption.CASCADE
        ).nullable()
    val name = varchar("name", 255)
    val price = double("price")
}


class ServiceDao(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(ServiceCategoriesTable, ServicesTable)
        }
    }

    suspend fun createService(serviceData: Service): Service = dbQuery {
        val generatedId = ServicesTable.insertAndGetId { row ->
            row[name] = serviceData.name
            row[price] = serviceData.price
            if (serviceData.category?.id != null) row[serviceCategoryId] =
                EntityID(serviceData.category.id, ServiceCategoriesTable)
        }
        serviceData.copy(id = generatedId.value)
    }

    suspend fun createCategory(categoryData: ServiceCategory): ServiceCategory = dbQuery {
        val generatedId = ServiceCategoriesTable.insertAndGetId { row ->
            row[name] = categoryData.name
        }
        categoryData.copy(id = generatedId.value)
    }


    suspend fun getServiceById(id: Int): Service? = dbQuery {
        (ServicesTable leftJoin ServiceCategoriesTable).select { ServicesTable.id eq id }.singleOrNull()?.let {
            tableToServiceData(it)
        }
    }

    suspend fun getAllService(
        page: Long? = null,
        size: Int? = null,
        filter: Map<String, String>? = null,
        sortField: String? = null,
        sortOrder: String? = null,
        operator: String? = null
    ): List<Service> = dbQuery {
        val query = (ServicesTable leftJoin ServiceCategoriesTable).selectAll()

        filter?.get("name")?.let {
            query.andWhere { ServicesTable.name like "%${it}%" }
        }
        filter?.get("price")?.toDoubleOrNull()?.let {
            when (operator) {
                "lessEq" -> query.andWhere { ServicesTable.price lessEq it }
                "greaterEq" -> query.andWhere { ServicesTable.price greaterEq it }
                "less" -> query.andWhere { ServicesTable.price less it }
                "greater" -> query.andWhere { ServicesTable.price greater it }
                else -> {
                    query.andWhere { ServicesTable.price eq it }
                }
            }
        }
        filter?.get("category")?.let {
            query.andWhere { ServiceCategoriesTable.name like "%${it}%" }
        }

        val order = getOrderByString(sortOrder)

        query.orderBy(
            when (sortField) {
                "name" -> ServicesTable.name to order
                "price" -> ServicesTable.price to order
                "category" -> ServiceCategoriesTable.name to order
                else -> {
                    ServicesTable.id to order
                }
            }
        )


        if (page != null && size != null) {
            query.limit(size, (page - 1) * size)
        }

        query.map { tableToServiceData(it) }
    }

    suspend fun getAllCategory(
        page: Long? = null,
        size: Int? = null,
        filter: Map<String, String>? = null,
        sortField: String? = null,
        sortOrder: String? = null
    ): List<ServiceCategory> = dbQuery {
        val query = ServiceCategoriesTable.selectAll()

        filter?.get("name")?.let {
            query.andWhere { ServiceCategoriesTable.name like "%${it}%" }
        }

        val order = getOrderByString(sortOrder)

        query.orderBy(
            when (sortField) {
                "name" -> ServiceCategoriesTable.name to order
                else -> {
                    ServiceCategoriesTable.id to order
                }
            }
        )

        if (page != null && size != null) {
            query.limit(size, (page - 1) * size)
        }

        query.map { tableToCategoryData(it) }
    }

    suspend fun deleteCategory(id: Int) = dbQuery {
        ServiceCategoriesTable.deleteWhere { ServiceCategoriesTable.id eq id }
    }

    suspend fun deleteService(id: Int) = dbQuery {
        ServicesTable.deleteWhere { ServicesTable.id eq id }
    }

    suspend fun updateCategory(id: Int, categoryData: ServiceCategory) = dbQuery {
        ServiceCategoriesTable.update({ ServiceCategoriesTable.id eq id }) {
            it[name] = categoryData.name
        }
    }

    suspend fun updateService(id: Int, serviceData: Service) = dbQuery {
        ServicesTable.update({ ServicesTable.id eq id }) {
            it[name] = serviceData.name
            it[price] = serviceData.price
            if (serviceData.category?.id != null) it[serviceCategoryId] =
                EntityID(serviceData.category.id, ServiceCategoriesTable)
        }
    }

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

    suspend fun getCategoryById(id: Int): ServiceCategory? = dbQuery {
        (ServiceCategoriesTable).select { ServicesTable.id eq id }.singleOrNull()?.let {
            tableToCategoryData(it)
        }
    }
}