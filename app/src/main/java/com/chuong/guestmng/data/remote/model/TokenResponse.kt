package com.chuong.guestmng.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "uid") val uid: Any? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "level") val level: Int? = null,
    @Json(name = "region") val region: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "open_id") val openId: String? = null,
    @Json(name = "server_url") val serverUrl: String? = null,
    @Json(name = "expires_at") val expiresAt: Double? = null,
    @Json(name = "error") val error: String? = null
)
