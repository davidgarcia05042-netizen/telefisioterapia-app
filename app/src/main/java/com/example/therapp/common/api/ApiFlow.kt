package com.example.therapp.common.api

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Antes cerraba el cuerpo del error antes de leerlo y lo interpretaba con un
 * formato que el backend no usa, así que todo error terminaba en un mensaje
 * técnico en inglés. Ahora el mensaje sale del cuerpo (ErrorMessages) y los
 * fallos de red tienen texto propio. El tope total cubre el arranque en frío
 * de Render (el servicio gratuito tarda hasta un minuto en despertar).
 */
fun <T> apiRequestFlow(
    timeOut: Long = 90_000L,
    call: suspend () -> Response<T>
): Flow<ApiResponse<T & Any>> = flow {
    emit(ApiResponse.Loading)
    val result: ApiResponse<T & Any> = withTimeoutOrNull(timeOut) {
        try {
            val response = call()
            val body = response.body()
            when {
                response.isSuccessful && body != null -> ApiResponse.Success(body)
                response.isSuccessful -> ApiResponse.Failure(ErrorMessages.UNEXPECTED, response.code())
                else -> ApiResponse.Failure(
                    ErrorMessages.fromResponse(response.code(), response.errorBody()?.string()),
                    response.code()
                )
            }
        } catch (e: SocketTimeoutException) {
            ApiResponse.Failure(ErrorMessages.TIMEOUT, ErrorMessages.CODE_TIMEOUT)
        } catch (e: IOException) {
            ApiResponse.Failure(ErrorMessages.NO_CONNECTION, ErrorMessages.CODE_NO_CONNECTION)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Por ejemplo, una respuesta que no se puede convertir al modelo.
            ApiResponse.Failure(ErrorMessages.UNEXPECTED, ErrorMessages.CODE_UNEXPECTED)
        }
    } ?: ApiResponse.Failure(ErrorMessages.TIMEOUT, ErrorMessages.CODE_TIMEOUT)
    emit(result)
}.flowOn(Dispatchers.IO)
