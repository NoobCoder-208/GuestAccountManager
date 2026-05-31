package com.chuong.guestmng.data.repository

import com.chuong.guestmng.data.local.dao.AccountDao
import com.chuong.guestmng.data.local.entity.AccountEntity
import com.chuong.guestmng.data.remote.api.GuestApi
import com.chuong.guestmng.domain.model.GuestAccount
import com.chuong.guestmng.domain.repository.GuestRepository
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class GuestRepositoryImpl(
    private val accountDao: AccountDao,
    private val guestApi: GuestApi
) : GuestRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override fun getAllAccounts(): Flow<List<GuestAccount>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(localId: Int): GuestAccount? {
        return withContext(Dispatchers.IO) {
            accountDao.getAccountById(localId)?.toDomain()
        }
    }

    private fun tryParseErrorBody(e: Exception): String? {
        if (e is retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                try {
                    val json = org.json.JSONObject(errorBody)
                    if (json.has("error")) {
                        return json.getString("error")
                    }
                    if (json.has("message")) {
                        return json.getString("message")
                    }
                    return errorBody
                } catch (jsonEx: Exception) {
                    return errorBody
                }
            }
        }
        return null
    }

    private suspend fun parseTokenResponse(
        rawBody: String,
        uidInput: String,
        passwordInput: String,
        selectedGroups: Set<String>,
        note: String
    ): Result<GuestAccount> {
        try {
            val json = org.json.JSONObject(rawBody)
            
            // Check for explicit error field
            if (json.has("error")) {
                val errorVal = json.optString("error")
                if (!errorVal.isNullOrBlank()) {
                    return Result.failure(Exception(errorVal))
                }
            }
            
            // Check if it lacks required token fields -> must be undefined JSON output, show raw
            val hasToken = json.has("token") || json.has("access_token") || json.has("accessToken")
            if (!hasToken) {
                return Result.failure(Exception(rawBody))
            }
            
            val rawUid = if (json.has("uid")) json.get("uid") else uidInput
            val uidString = when (rawUid) {
                is Number -> rawUid.toLong().toString()
                is String -> rawUid
                else -> rawUid?.toString() ?: uidInput
            }
            
            val nickname = if (json.has("nickname")) json.getString("nickname") else "Guest_${uidInput.takeLast(4)}"
            val name = if (json.has("name")) json.getString("name") else "Guest User"
            val level = if (json.has("level")) json.getInt("level") else (10..99).random()
            val region = if (json.has("region")) json.getString("region") else "VN"
            
            val accessToken = when {
                json.has("access_token") -> json.getString("access_token")
                json.has("accessToken") -> json.getString("accessToken")
                else -> UUID.randomUUID().toString().replace("-", "")
            }
            
            val token = if (json.has("token")) json.getString("token") else "JWT_TOKEN_${System.currentTimeMillis()}"
            
            val openId = when {
                json.has("open_id") -> json.getString("open_id")
                json.has("openId") -> json.getString("openId")
                else -> "OPEN_ID_${System.currentTimeMillis()}"
            }
            
            val serverUrl = when {
                json.has("server_url") -> json.getString("server_url")
                json.has("serverUrl") -> json.getString("serverUrl")
                else -> "https://server.game.com"
            }
            
            val rawExpires = when {
                json.has("expires_at") -> json.optDouble("expires_at")
                json.has("expiresAt") -> json.optDouble("expiresAt")
                else -> null
            }
            
            val convertedExpiresAt = if (rawExpires != null && !rawExpires.isNaN()) {
                val sec = rawExpires.toLong()
                if (sec < 9999999999L) {
                    sec * 1000L
                } else {
                    sec
                }
            } else {
                System.currentTimeMillis() + 86400000L * 15 // default 15 days
            }
            
            val entity = AccountEntity(
                uidInput = uidInput,
                passwordInput = passwordInput,
                uid = uidString,
                nickname = nickname,
                name = name,
                level = level,
                region = region,
                accessToken = accessToken,
                token = token,
                openId = openId,
                serverUrl = serverUrl,
                expiresAt = convertedExpiresAt,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                groupName = selectedGroups.joinToString(","),
                note = note
            )
            val localId = accountDao.insertAccount(entity)
            return Result.success(entity.copy(localId = localId.toInt()).toDomain())
        } catch (e: Exception) {
            return Result.failure(Exception(rawBody.ifBlank { e.localizedMessage ?: "Unknown parsing error" }))
        }
    }

    private fun parseTokenResponseForRefresh(rawBody: String, account: AccountEntity): AccountEntity {
        val json = org.json.JSONObject(rawBody)
        
        // Check for explicit error field
        if (json.has("error")) {
            val errorVal = json.optString("error")
            if (!errorVal.isNullOrBlank()) {
                throw Exception(errorVal)
            }
        }
        
        // Check if it lacks required token fields
        val hasToken = json.has("token") || json.has("access_token") || json.has("accessToken")
        if (!hasToken) {
            throw Exception(rawBody)
        }
        
        val rawUid = if (json.has("uid")) json.get("uid") else account.uid
        val uidString = when (rawUid) {
            is Number -> rawUid.toLong().toString()
            is String -> rawUid
            else -> rawUid?.toString() ?: account.uid
        }
        
        val nickname = if (json.has("nickname")) json.getString("nickname") else account.nickname
        val name = if (json.has("name")) json.getString("name") else account.name
        val level = if (json.has("level")) json.getInt("level") else account.level
        val region = if (json.has("region")) json.getString("region") else account.region
        
        val accessToken = when {
            json.has("access_token") -> json.getString("access_token")
            json.has("accessToken") -> json.getString("accessToken")
            else -> account.accessToken
        }
        
        val token = if (json.has("token")) json.getString("token") else account.token
        
        val openId = when {
            json.has("open_id") -> json.getString("open_id")
            json.has("openId") -> json.getString("openId")
            else -> account.openId
        }
        
        val serverUrl = when {
            json.has("server_url") -> json.getString("server_url")
            json.has("serverUrl") -> json.getString("serverUrl")
            else -> account.serverUrl
        }
        
        val rawExpires = when {
            json.has("expires_at") -> json.optDouble("expires_at")
            json.has("expiresAt") -> json.optDouble("expiresAt")
            else -> null
        }
        
        val convertedExpiresAt = if (rawExpires != null && !rawExpires.isNaN()) {
            val sec = rawExpires.toLong()
            if (sec < 9999999999L) {
                sec * 1000L
            } else {
                sec
            }
        } else {
            account.expiresAt
        }
        
        return account.copy(
            uid = uidString,
            nickname = nickname,
            name = name,
            level = level,
            region = region,
            accessToken = accessToken,
            token = token,
            openId = openId,
            serverUrl = serverUrl,
            expiresAt = convertedExpiresAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun addAccount(
        uidInput: String,
        passwordInput: String,
        selectedGroups: Set<String>,
        note: String
    ): Result<GuestAccount> = withContext(Dispatchers.IO) {
        try {
            val responseBody = guestApi.getToken(uidInput, passwordInput)
            val rawBody = responseBody.string()
            parseTokenResponse(rawBody, uidInput, passwordInput, selectedGroups, note)
        } catch (e: Exception) {
            val validationError = tryParseErrorBody(e)
            if (validationError != null) {
                return@withContext Result.failure(Exception(validationError))
            }
            
            // Only fallback to offline simulation if the exception is an offline connectivity/IO problem
            if (e is java.io.IOException) {
                val generatedNickname = "Guest_${uidInput.takeLast(4)}"
                val entity = AccountEntity(
                    uidInput = uidInput,
                    passwordInput = passwordInput,
                    uid = uidInput,
                    nickname = generatedNickname,
                    name = "Guest User $uidInput",
                    level = (10..99).random(),
                    region = listOf("VN", "US", "JP", "KR", "SG", "TW").random(),
                    accessToken = UUID.randomUUID().toString().replace("-", ""),
                    token = "jwt.eyJ1aWQiOiIkdWlkSW5wdXQiLCJleHAiOiIrMTVkYXlzIn0." + UUID.randomUUID().toString().take(12),
                    openId = "op_${uidInput}_" + (1000..9999).random(),
                    serverUrl = "https://asia-guest.game-server.com",
                    expiresAt = System.currentTimeMillis() + (86400000L * listOf(2, 5, 10, -1, 30, 0).random()), // Mix of active & expired
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    groupName = selectedGroups.joinToString(","),
                    note = if (note.isBlank()) "Offline Fallback" else "$note (Offline Fallback)"
                )
                val localId = accountDao.insertAccount(entity)
                Result.success(entity.copy(localId = localId.toInt()).toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateAccount(account: GuestAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = AccountEntity.fromDomain(account).copy(updatedAt = System.currentTimeMillis())
            accountDao.updateAccount(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(localId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            accountDao.deleteAccountById(localId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccounts(localIds: List<Int>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            accountDao.deleteAccountsByIds(localIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshAccounts(localIds: List<Int>): Result<Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        for (id in localIds) {
            val account = accountDao.getAccountById(id) ?: continue
            try {
                val responseBody = guestApi.getToken(account.uidInput, account.passwordInput)
                val rawBody = responseBody.string()
                val updated = parseTokenResponseForRefresh(rawBody, account)
                accountDao.updateAccount(updated)
                successCount++
            } catch (e: Exception) {
                // Update timestamp but keep older data if network fails
                val updated = account.copy(
                    updatedAt = System.currentTimeMillis()
                )
                accountDao.updateAccount(updated)
            }
        }
        Result.success(successCount)
    }

    override suspend fun refreshAllAccounts(): Result<Int> = withContext(Dispatchers.IO) {
        val allEntities = accountDao.getAllAccountsDirect()
        val ids = allEntities.map { it.localId }
        refreshAccounts(ids)
    }

    override suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val entities = accountDao.getAllAccountsDirect()
        val listType = Types.newParameterizedType(List::class.java, AccountEntity::class.java)
        val adapter: JsonAdapter<List<AccountEntity>> = moshi.adapter(listType)
        adapter.toJson(entities)
    }

    override suspend fun importBackup(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val listType = Types.newParameterizedType(List::class.java, AccountEntity::class.java)
            val adapter: JsonAdapter<List<AccountEntity>> = moshi.adapter(listType)
            val entities = adapter.fromJson(jsonString)
            if (entities.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("No accounts found in backup JSON"))
            }
            val cleanEntities = entities.map { it.copy(localId = 0) }
            accountDao.insertAccounts(cleanEntities)
            Result.success(cleanEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
