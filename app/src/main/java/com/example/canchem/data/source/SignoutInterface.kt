package com.example.canchem.data.source

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.POST

interface SignoutInterface {
    @POST("api/login/naver") // 변경해야됨.
    fun signout(
        @Header("Authorization") accessToken: String?,  // Baerer AccessToken
    ) : Call<Token>
}