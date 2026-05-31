package com.chuong.guestmng.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chuong.guestmng.data.local.dao.AccountDao
import com.chuong.guestmng.data.local.entity.AccountEntity

@Database(entities = [AccountEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
