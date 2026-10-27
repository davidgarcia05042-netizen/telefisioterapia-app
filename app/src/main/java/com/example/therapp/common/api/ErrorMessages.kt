package com.example.therapp.common.api

import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * Mensajes de error para el paciente.
 *
 * El backend responde los errores como {"detail": "..."} (permisos, cambio de
 * contraseña) o con los errores del serializador: {"non_field_errors": [...]}
 * o {"campo": ["..."]}.
 */
object ErrorMessages {
    const val NO_CONNECTION = "No se pudo conectar con el servidor. Revisa tu conexión a internet."
    const val TIMEOUT = "El servidor tardó demasiado en responder. Inténtalo de nuevo en un momento."
    const val UNEXPECTED = "Ocurrió un error inesperado. Inténtalo de nuevo."
    const val SERVER_UNAVAILABLE = "El servidor no está disponible en este momento. Inténtalo de nuevo en unos segundos."

    // Códigos propios para fallos sin respuesta HTTP.
    const val CODE_NO_CONNECTION = 0
    const val CODE_TIMEOUT = 408
    const val CODE_UNEXPECTED = -1

    /**
     * Mensaje de una respuesta de error. Un 5xx sin mensaje propio suele ser
     * la página HTML de Render mientras el backend se reinicia (en M5, un 502
     * de unos 20 s): antes terminaba en "error inesperado".
     */
    fun fromResponse(code: Int, body: String?): String =
        fromBody(body) ?: if (code >= 500) SERVER_UNAVAILABLE else UNEXPECTED

    /** Fallo que suele pasar solo: sin red, demora o servidor caído un momento. */
    fun isTransient(code: Int): Boolean =
        code >= 500 || code == CODE_NO_CONNECTION || code == CODE_TIMEOUT

    /** Primer mensaje legible del cuerpo de un error, o null si no hay. */
    fun fromBody(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val json = try {
            // Gson 2.8.5 (el que trae converter-gson 2.9) no tiene parseString.
            @Suppress("DEPRECATION")
            JsonParser().parse(body)
        } catch (e: Exception) {
            return null
        }
        // Gson acepta texto suelto como JSON; una página HTML de error no es un mensaje.
        if (json.isJsonArray) return firstText(json)
        if (!json.isJsonObject) return null
        val fields = json.asJsonObject
        fields.get("detail")?.let { detail -> firstText(detail)?.let { return it } }
        fields.get("non_field_errors")?.let { errors -> firstText(errors)?.let { return it } }
        return fields.entrySet().firstNotNullOfOrNull { firstText(it.value) }
    }

    private fun firstText(element: JsonElement): String? = when {
        element.isJsonPrimitive && element.asJsonPrimitive.isString ->
            element.asString.takeIf { it.isNotBlank() }
        element.isJsonArray -> element.asJsonArray.firstNotNullOfOrNull { firstText(it) }
        // {"errors": [{"exercise_id": "..."}]} de POST /exercise_results.
        element.isJsonObject -> element.asJsonObject.entrySet().firstNotNullOfOrNull { firstText(it.value) }
        else -> null
    }
}
