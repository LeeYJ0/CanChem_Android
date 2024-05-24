package com.example.canchem.ui.home

import com.example.canchem.data.source.dataclass.Search.SearchResultResponse
import com.example.canchem.data.source.myinterface.Search.SearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Call
import retrofit2.awaitResponse
import android.content.Context


object RetrofitClient {
    private const val BASE_URL = "http://13.124.223.31:8080"

    private val service: SearchService by lazy {
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(SearchService::class.java)
    }

    suspend fun fetchChemicals(context: Context, token: String, searchQuery:String, page:Int): SearchResultResponse {
        return withContext(Dispatchers.IO) {
            val call = service.getCompounds(token, searchQuery, page)
            val response = call.awaitResponse()
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Empty response body")
            } else {
                throw Exception("Error: ${response.code()} - ${response.message()}")
            }
        }
    }
}
