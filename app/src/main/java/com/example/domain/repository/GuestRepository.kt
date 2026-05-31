package com.example.domain.repository

import com.example.domain.model.GuestAccount
import kotlinx.coroutines.flow.Flow

interface GuestRepository {
    fun getAllAccounts(): Flow<List<GuestAccount>>
    
    suspend fun getAccountById(localId: Int): GuestAccount?
    
    suspend fun addAccount(
        uidInput: String,
        passwordInput: String,
        selectedGroups: Set<String>,
        note: String
    ): Result<GuestAccount>
    
    suspend fun updateAccount(account: GuestAccount): Result<Unit>
    
    suspend fun deleteAccount(localId: Int): Result<Unit>
    
    suspend fun deleteAccounts(localIds: List<Int>): Result<Unit>
    
    suspend fun refreshAccounts(localIds: List<Int>): Result<Int>
    
    suspend fun refreshAllAccounts(): Result<Int>
    
    suspend fun exportBackup(): String
    
    suspend fun importBackup(jsonString: String): Result<Int>
}
