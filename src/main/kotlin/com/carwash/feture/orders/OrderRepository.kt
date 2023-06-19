package com.carwash.feture.orders

import com.carwash.ResultResponse
import com.carwash.feture.cars.CarsDao
import com.carwash.feture.service.ServiceDao
import com.carwash.feture.users.UserDao

class OrderRepository(val dao: OrdersDao, val serviceDao: ServiceDao, val userDao: UserDao, val carDao: CarsDao) {
    suspend fun getALlOrders(
        page: Long?,
        size: Int?,
        sortField: String?,
        sortOrder: String?,
        filter: Map<String?, String?>?,
        operatorStartDate: String?,
        operatorEndDate: String?
    ): List<SimpleOrder> {

        val newFilter = mutableMapOf<String, String>()

        filter?.forEach { (key, value) ->
            if (key != null && value != null) {
                newFilter[key] = value
            }
        }

        return dao.getAllOrders(
            page = page,
            size = size,
            filter = newFilter,
            sortField = sortField,
            sortOrder = sortOrder,
            operatorStartDate,
            operatorEndDate
        )
    }

    suspend fun createOrder(order: OrderRequest, id: Int?): ResultResponse<SimpleOrder> {
        if (order.serviceId == null || order.customerCarId == null || order.employeeId == null || id == null) {
            return ResultResponse.Error("Data incorrect")
        }

        if (order.startDate == null || order.endDate == null) {
            return ResultResponse.Error("Data incorrect")
        }

        val service = serviceDao.getServiceById(order.serviceId)
        val customerCar = carDao.getCustomerCarById(order.customerCarId)
        val employee = userDao.getUserById(order.employeeId)
        val administrator = userDao.getUserById(id)

        if (service == null || customerCar == null || employee == null || administrator == null) {
            return ResultResponse.Error("Data incorrect")
        }

        val created = dao.createOrder(
            Order(
                service = service,
                customerCar = customerCar,
                employee = employee,
                administrator = administrator,
                status = order.status ?: OrderStatus.IN_PROGRESS,
                startDate = order.startDate,
                endDate = order.endDate
            )
        )?.toSimple()

        return if (created == null) {
            ResultResponse.Error("Order not created")
        } else {
            ResultResponse.Success(created)
        }

    }

    suspend fun deleteOrder(id: Int): Boolean {
        return dao.deleteOrder(id) > 0
    }

    suspend fun updateOrder(id: Int, order: OrderRequest): Boolean {
        return dao.updateOrder(id = id, order = order) > 0
    }


}