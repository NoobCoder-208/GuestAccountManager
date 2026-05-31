package com.chuong.guestmng.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chuong.guestmng.domain.model.GuestAccount

@Entity(tableName = "guest_accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val uidInput: String,
    val passwordInput: String,
    val uid: String,
    val nickname: String,
    val name: String,
    val level: Int,
    val region: String,
    val accessToken: String,
    val token: String,
    val openId: String,
    val serverUrl: String,
    val expiresAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val groupName: String, // Comma-separated list of groups, e.g. "Main,Farm"
    val note: String
) {
    fun toDomain(): GuestAccount {
        val groupNames = if (groupName.isBlank()) {
            emptySet()
        } else {
            groupName.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        return GuestAccount(
            localId = localId,
            uidInput = uidInput,
            passwordInput = passwordInput,
            uid = uid,
            nickname = nickname,
            name = name,
            level = level,
            region = region,
            accessToken = accessToken,
            token = token,
            openId = openId,
            serverUrl = serverUrl,
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            groupNames = groupNames,
            note = note
        )
    }

    companion object {
        fun fromDomain(domain: GuestAccount): AccountEntity {
            return AccountEntity(
                localId = domain.localId,
                uidInput = domain.uidInput,
                passwordInput = domain.passwordInput,
                uid = domain.uid,
                nickname = domain.nickname,
                name = domain.name,
                level = domain.level,
                region = domain.region,
                accessToken = domain.accessToken,
                token = domain.token,
                openId = domain.openId,
                serverUrl = domain.serverUrl,
                expiresAt = domain.expiresAt,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                groupName = domain.groupNames.joinToString(","),
                note = domain.note
            )
        }
    }
}
