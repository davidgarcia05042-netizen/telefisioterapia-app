package com.example.therapp.data.auth.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class UserRes(
    val url: String? = null,
    val id: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val groups: List<GroupRes>? = null,
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("last_name")
    val lastName: String? = null,
)
