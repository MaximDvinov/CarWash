package com.carwash.feture.users

import at.favre.lib.crypto.bcrypt.BCrypt
import com.carwash.db.dbQuery
import com.carwash.db.upsert
import com.carwash.feture.users.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object UsersTable : IntIdTable() {
    val firstName = varchar("first_name", 255)
    val lastName = varchar("last_name", 255)
    val patronymic = varchar("patronymic", 255).nullable()
    val email = varchar("email", 255)
    val password = varchar("password", 60)
    val isSendNotify = bool("is_send_notify").nullable()
}

object RolesTable : IntIdTable() {
    val name = varchar("name", 255)
}

object RoleUserTable : IntIdTable() {
    val userId = reference(name = "user_id", foreign = UsersTable, onDelete = ReferenceOption.CASCADE)
    val roleId = reference(name = "role_id", foreign = RolesTable, onDelete = ReferenceOption.CASCADE)
}

object TokensTable : Table() {
    val userId = reference(name = "user_id", foreign = UsersTable, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val token = varchar("token", 36).uniqueIndex()

    override val primaryKey = PrimaryKey(token, name = "token_pk")
}

class UserDao(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(UsersTable, RolesTable, RoleUserTable, TokensTable)
        }
    }

    suspend fun createUser(userData: User): User = dbQuery {
        val generatedId = UsersTable.insertAndGetId { row ->
            row[firstName] = userData.firstName
            row[lastName] = userData.lastName
            row[patronymic] = userData.patronymic
            row[email] = userData.email
            row[isSendNotify] = userData.isSendNotify
            row[password] = BCrypt.withDefaults().hashToString(12, userData.password.toCharArray())
        }

        userData.copy(id = generatedId.value)
    }

    suspend fun getUserByEmail(email: String): User? = dbQuery {
        UsersTable.select { UsersTable.email eq email }
            .mapNotNull { tableToUserData(row = it) }
            .singleOrNull()
    }

    suspend fun getPasswordUserByEmail(email: String): String? = dbQuery {
        UsersTable.select { UsersTable.email eq email }
            .mapNotNull { it[UsersTable.password] }
            .singleOrNull()
    }

    suspend fun getUserById(id: Int): User? = dbQuery {
        UsersTable.select { UsersTable.id eq id }
            .mapNotNull { tableToUserData(row = it) }
            .singleOrNull()
    }

    suspend fun getAllUser(): List<UserResponse> = dbQuery {
        UsersTable.selectAll().map { tableToUserData(row = it).toResponse() }
    }

    suspend fun updateUser(id: Int, userData: User): Int = dbQuery {
        UsersTable.update({ UsersTable.id eq userData.id }) { row ->
            row[firstName] = userData.firstName
            row[lastName] = userData.lastName
            row[patronymic] = userData.patronymic
            row[email] = userData.email
            row[isSendNotify] = userData.isSendNotify
        }
    }

    suspend fun deleteUser(id: Int) = dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id }
    }

    suspend fun getAllRoles(): List<Role> = dbQuery {
        RolesTable.selectAll().map { row ->
            Role(
                id = row[RolesTable.id].value,
                name = row[RolesTable.name]
            )
        }
    }


    suspend fun updateRole(user: Int, role: Int) = dbQuery {
        RoleUserTable.upsert({ RoleUserTable.userId eq user }) { row ->
            row[userId] = EntityID(user, UsersTable)
            row[roleId] = EntityID(role, RolesTable)
        }
    }

    suspend fun deleteUserRole(user: Int, role: Int) = dbQuery {
        RoleUserTable.deleteWhere {
            (userId eq user) and (roleId eq role)
        }
    }

    private suspend fun getUserRoles(userId: Int): Role? = dbQuery {
        val userRole = (RoleUserTable innerJoin RolesTable)
            .select { RoleUserTable.userId eq userId }.singleOrNull()

        userRole?.let {
            Role(
                id = it[RolesTable.id].value,
                name = it[RolesTable.name]
            )
        }
    }


    suspend fun tableToUserData(alias: Alias<UsersTable>? = null, row: ResultRow): User {
        return if (alias != null) {
            User(
                id = row[alias[UsersTable.id]].value,
                firstName = row[alias[UsersTable.firstName]],
                lastName = row[alias[UsersTable.lastName]],
                patronymic = row[alias[UsersTable.patronymic]],
                email = row[alias[UsersTable.email]],
                isSendNotify = row[alias[UsersTable.isSendNotify]],
                roles = getUserRoles(row[alias[UsersTable.id]].value),
                password = row[alias[UsersTable.password]]
            )
        } else {
            User(
                id = row[UsersTable.id].value,
                firstName = row[UsersTable.firstName],
                lastName = row[UsersTable.lastName],
                patronymic = row[UsersTable.patronymic],
                email = row[UsersTable.email],
                isSendNotify = row[UsersTable.isSendNotify],
                roles = getUserRoles(row[UsersTable.id].value),
                password = row[UsersTable.password]
            )
        }
    }

    suspend fun saveToken(user: Int, userToken: String) = dbQuery {
        val oldToken = TokensTable.select { TokensTable.userId eq user }.singleOrNull()
        if (oldToken == null) {
            TokensTable.insertIgnore { row ->
                row[userId] = EntityID(user, UsersTable)
                row[token] = userToken
            }
            getUserById(user)?.let { TokenWithUser(user = it.toResponse(), token = userToken) }
        } else {
            TokensTable.update({ TokensTable.userId eq user }) { row ->
                row[token] = userToken
            }
            getUserById(user)?.let { TokenWithUser(user = it.toResponse(), token = userToken) }
        }
    }

    suspend fun getUserWithToken(userToken: String): TokenWithUser? = dbQuery {
        val tokenTable =
            (TokensTable innerJoin UsersTable).select { TokensTable.token eq userToken }.singleOrNull()
                ?: return@dbQuery null

        TokenWithUser(
            token = tokenTable[TokensTable.token],
            user = tableToUserData(row = tokenTable).toResponse()
        )
    }

}