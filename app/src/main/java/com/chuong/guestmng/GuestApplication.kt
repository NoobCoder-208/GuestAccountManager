package com.chuong.guestmng

import android.app.Application
import androidx.room.Room
import com.chuong.guestmng.data.local.db.AppDatabase
import com.chuong.guestmng.data.remote.api.GuestApi
import com.chuong.guestmng.data.repository.GuestRepositoryImpl
import com.chuong.guestmng.domain.repository.GuestRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GuestApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: GuestRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Room SQLite Local Database setup
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "guest_manager.db"
        ).fallbackToDestructiveMigration().build()

        // 2. Retrofit Network Client setup
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://prmjet.vercel.app/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val guestApi = retrofit.create(GuestApi::class.java)

        // 3. Repository with DB and API dependency injection
        repository = GuestRepositoryImpl(database.accountDao(), guestApi)
    }
}
