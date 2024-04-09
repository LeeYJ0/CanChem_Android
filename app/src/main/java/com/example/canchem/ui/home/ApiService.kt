package com.example.canchem

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST
import retrofit2.http.Path

interface MoleculeApiService {
   @GET("/getMolInfo_Name")
   fun getCompound(@Query("mol_Name") searchQuery: String): Call<mol_Name>
}