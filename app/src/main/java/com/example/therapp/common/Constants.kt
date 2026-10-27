package com.example.therapp.common

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.therapp.BuildConfig
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 7/30/2025
 * @version 1.0
 */
// Se define en app/build.gradle.kts (buildConfigField). Antes apuntaba fijo a
// http://10.0.2.2:8000, el backend local visto desde el emulador.
val HOST_URL: String = BuildConfig.HOST_URL


@RequiresApi(Build.VERSION_CODES.O)
fun String.toReadableDate(): String {
    return try {
        // En la zona del teléfono (el backend la manda en hora de Colombia).
        val dateTime = OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern(
            "EEEE, d 'de' MMMM 'de' yyyy 'a las' h:mm a",
            Locale("es", "CO")
        )
        dateTime.format(formatter)
    } catch (e: Exception) {
        this // si falla el parseo, devuelve el string original
    }
}