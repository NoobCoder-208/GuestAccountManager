package com.chuong.guestmng.domain.model

data class GuestAccount(
    val localId: Int = 0,
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
    val groupNames: Set<String> = emptySet(),
    val note: String
) {
    fun isExpired(): Boolean = expiresAt > 0 && expiresAt <= System.currentTimeMillis()
    
    fun isNearExpired(): Boolean {
        if (expiresAt <= 0) return false
        val diff = expiresAt - System.currentTimeMillis()
        // Near expiration is defined as expiring within 24 hours (24 * 3600 * 1000 = 86,400,000 ms)
        return diff in 1..86400000L
    }
    
    fun isActiveToken(): Boolean {
        // Active means not expired yet
        return expiresAt > System.currentTimeMillis()
    }
}
