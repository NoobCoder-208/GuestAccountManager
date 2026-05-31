package com.example.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface GuestApi {
    @GET("token")
    suspend fun getToken(
        @Query("uid") uid: String,
        @Query("password") password: String
    ): ResponseBody
}
