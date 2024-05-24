package com.example.canchem.data.source.myinterface.Search

import com.example.canchem.data.source.dataclass.Search.SearchResultResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SearchService {
    @GET("/api/search/chem")
    fun getCompounds(
        @Header("Authorization") token: String,
        @Query("keyword") searchQuery: String,
        @Query("page") page: Int
    ): Call<SearchResultResponse>
}