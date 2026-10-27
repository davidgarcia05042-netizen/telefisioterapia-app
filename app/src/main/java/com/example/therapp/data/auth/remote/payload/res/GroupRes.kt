package com.example.therapp.data.auth.remote.payload.res

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class GroupRes(
    val name: String? = null,
    val permissions: List<PermissionRes>? = null,
)
