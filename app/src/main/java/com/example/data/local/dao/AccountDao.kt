package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM guest_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM guest_accounts WHERE localId = :localId")
    suspend fun getAccountById(localId: Int): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM guest_accounts WHERE localId = :localId")
    suspend fun deleteAccountById(localId: Int)

    @Query("DELETE FROM guest_accounts WHERE localId IN (:localIds)")
    suspend fun deleteAccountsByIds(localIds: List<Int>)

    @Query("SELECT * FROM guest_accounts")
    suspend fun getAllAccountsDirect(): List<AccountEntity>
}
