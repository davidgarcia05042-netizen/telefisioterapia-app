package com.example.therapp.data.auth.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class PermissionRes(
    val id: Int? = null,
    val name: String? = null,
    val codename: String? = null,
    @SerializedName("content_type")
    val contentType: Int? = null,
)
