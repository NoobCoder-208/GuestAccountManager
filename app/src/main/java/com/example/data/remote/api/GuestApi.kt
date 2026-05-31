package com.example.data.remote.api

import com.example.data.remote.model.TokenResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GuestApi {
    @GET("token")
    suspend fun getToken(
        @Query("uid") uid: String,
        @Query("password") password: String
    ): TokenResponse
}
