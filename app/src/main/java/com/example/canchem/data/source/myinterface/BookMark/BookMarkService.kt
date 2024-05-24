package com.example.canchem.data.source.myinterface.BookMark

import com.example.canchem.data.source.dataclass.BookMark.BookMark
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BookMarkService {
    @GET("bookmark")
    fun getBookmark(@Query("token") token: String, @Query("state") state: Boolean): Call<BookMark>
}