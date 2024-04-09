package com.example.canchem.data.source

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GoogleLoginInterface {
    @POST("api/login/google")
    fun getLoginToken(
        //@Header("Authorization") accessToken: String?,  // Baerer AccessToken
        @Body googleToken: GoogleToken
    ) : Call<Token>
}