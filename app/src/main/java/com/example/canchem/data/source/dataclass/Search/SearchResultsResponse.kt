package com.example.canchem.data.source.dataclass.Search

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResultResponse(
    @SerializedName("total_elements") val totalElements: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("page_number") val pageNumber: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("search_results") val searchResults: ArrayList<Chemical>
)

@Serializable
data class Chemical(
    @SerializedName("id") val id: String,
    @SerializedName("cid") val cid: Int,
    @SerializedName("inpac_name") val inpacName: String,
    @SerializedName("molecular_formula") val molecularFormula: String,
    @SerializedName("molecular_weight") val molecularWeight: Double,
    @SerializedName("isomeric_smiles") val isomericSmiles: String,
    @SerializedName("inchi") val inchi: String,
    @SerializedName("inchiKey") val inchiKey: String,
    @SerializedName("canonical_smiles") val canonicalSmiles: String,
    @SerializedName("description") val description: String,
    @SerializedName("image_2D_url") val image2DUri: String?,
    @SerializedName("image_3D_conformer") val image3DConformer: Image3DConformer?
)

@Serializable
data class Image3DConformer(
    @SerializedName("bonds")
    val bonds: List<Int>,
    @SerializedName("coords")
    val coords: List<Double>
)
