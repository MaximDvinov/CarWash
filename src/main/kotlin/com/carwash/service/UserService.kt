package com.carwash.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.carwash.db.dao.*
import com.carwash.models.*
import java.util.UUID

class UserService(private val userDao: UserDao) {
    suspend fun getUserById(id: Int?): UserResponse? {
        return id?.let { userDao.getUserById(it)?.toResponse() }
    }

    suspend fun createUser(user: User): UserResponse? {
        val emailUser = userDao.getUserByEmail(user.email)

        return if (emailUser == null) {
            userDao.createUser(user).toResponse()
        } else {
            null
        }
    }

    suspend fun updateUser(id: Int, user: User): UserResponse {
        return userDao.updateUser(id, user).toResponse()
    }

    suspend fun updateRole(user: Int, role: Int) {
        userDao.updateRole(user, role)
    }

    suspend fun getAllRole(): List<Role> {
        return userDao.getAllRoles()
    }

    suspend fun getAllUser(): List<UserResponse> {
        return userDao.getAllUser()
    }

    suspend fun deleteUser(id: Int): Int {
        return userDao.deleteUser(id)
    }

    suspend fun authUser(data: EmailWithPassword): TokenWithUser? {
        val user = userDao.getUserByEmail(data.email) ?: return null

        return if (BCrypt.verifyer().verify(data.password.toCharArray(), user.password).verified) {
            user.id?.let { userDao.saveToken(it, generateToken()) }
        } else {
            null
        }
    }

    suspend fun validateToken(token: String): TokenWithUser? {
        return userDao.getUserWithToken(token)
    }

    private fun generateToken(): String {
        return UUID.randomUUID().toString()
    }


}