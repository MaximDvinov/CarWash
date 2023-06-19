package com.carwash.feture.service

class ServiceRepository(private val dao: ServiceDao) {
    suspend fun createService(serviceData: Service): Service = dao.createService(serviceData)

    suspend fun createCategory(categoryData: ServiceCategory): ServiceCategory = dao.createCategory(categoryData)

    suspend fun getAllService(
        page: Long? = null,
        size: Int? = null,
        sortField: String? = null,
        sortOrder: String? = null,
        filter: Map<String?, String?>? = null,
        operator: String? = null
    ): List<Service> {
        val newFilter = mutableMapOf<String, String>()

        filter?.forEach { (key, value) ->
            if (key != null && value != null) {
                newFilter[key] = value
            }
        }

        return dao.getAllService(
            page = page,
            size = size,
            filter = newFilter,
            sortField = sortField,
            sortOrder = sortOrder,
            operator
        )
    }


    suspend fun getAllCategory(
        page: Long? = null,
        size: Int? = null,
        sortField: String? = null,
        sortOrder: String? = null,
        filter: Map<String?, String?>? = null
    ): List<ServiceCategory> {
        val newFilter = mutableMapOf<String, String>()

        filter?.forEach { (key, value) ->
            if (key != null && value != null) {
                newFilter[key] = value
            }
        }

        return dao.getAllCategory(
            page = page,
            size = size,
            filter = newFilter,
            sortField = sortField,
            sortOrder = sortOrder
        )
    }


    suspend fun getServiceById(id: Int): Service? = dao.getServiceById(id)

    suspend fun updateService(id: Int, serviceData: Service): Boolean {
        return dao.updateService(id, serviceData) > 0
    }

    suspend fun updateCategory(id: Int, categoryData: ServiceCategory): Boolean {
        return dao.updateCategory(id, categoryData) > 0
    }

    suspend fun deleteCategory(id: Int): Boolean {
        return dao.deleteCategory(id) == 1
    }

    suspend fun deleteService(id: Int): Boolean {
        return dao.deleteService(id) == 1
    }

    suspend fun getCategoryById(id: Int):ServiceCategory? {
        return dao.getCategoryById(id)
    }

}