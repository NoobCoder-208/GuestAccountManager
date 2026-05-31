package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "uid") val uid: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "level") val level: Int? = null,
    @Json(name = "region") val region: String? = null,
    @Json(name = "accessToken") val accessToken: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "openId") val openId: String? = null,
    @Json(name = "serverUrl") val serverUrl: String? = null,
    @Json(name = "expiresAt") val expiresAt: Long? = null
)
