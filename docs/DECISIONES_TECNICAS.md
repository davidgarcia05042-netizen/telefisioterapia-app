# Decisiones técnicas — App del paciente (TherApp)

Reúne el contexto de la app, las decisiones de diseño con su justificación, el
contrato del flujo del paciente con la plataforma, las reglas de seguridad y
cómo compilarla. La integración se hizo por fases (M0 a M5); los planes de
trabajo de la plataforma están resumidos en `docs/DECISIONES_TECNICAS.md` de
[`telefisioterapia-backend`](https://github.com/davidgarcia05042-netizen/telefisioterapia-backend).

## Contexto

Trabajo de grado (Universidad Antonio Nariño, Ingeniería de Sistemas) de Juan
David García Rubio y Camila Sandoval Ruiz, dirigido por Ph.D. David Alberto
Herrera Álvarez, grupo de investigación LACSER/Bioingeniería.

La app es una **copia propia** de `santivarelaagent-cmd/therapp` (commit
`e3623fb`, 2026-03-06), hecha el 2026-10-19 para cerrar el flujo del paciente:
el terapeuta configura terapias, rutinas y ejercicios en el panel; el paciente
hace la rutina con la app y envía su video; el **Servicio de Procesamiento** lo
mide y el terapeuta revisa el resultado. Antes, el paciente se simulaba desde el
panel ("Subir video del paciente"): la app original subía el video a Firebase y
nunca avisaba al backend.

**Regla que no cambia:** la medición la hace el servicio (MediaPipe Heavy, 2D),
igual que para los videos de referencia del terapeuta. La app conserva su
detección en vivo solo como **guía visual** y nunca envía ángulos.

Repositorios relacionados:
[`telefisioterapia-backend`](https://github.com/davidgarcia05042-netizen/telefisioterapia-backend),
[`telefisioterapia-panel`](https://github.com/davidgarcia05042-netizen/telefisioterapia-panel),
[`servicio-procesamiento-pose`](https://github.com/davidgarcia05042-netizen/servicio-procesamiento-pose),
[`entorno-pruebas`](https://github.com/davidgarcia05042-netizen/entorno-pruebas) y
[`pose-benchmark`](https://github.com/davidgarcia05042-netizen/pose-benchmark).

**Tecnología:** Android nativa, Kotlin + Jetpack Compose, Hilt, Retrofit/OkHttp,
Room, CameraX, Media3/ExoPlayer y MediaPipe Tasks (modelos lite, full y heavy en
`assets`, solo para la guía en vivo). `minSdk` 24, `compileSdk` 35.

## Lo que se encontró en la app original

| # | Hallazgo | Consecuencia |
|---|---|---|
| A1 | `HOST_URL` fijo a `http://10.0.2.2:8000` (emulador local) | Se configura por variable de compilación y en HTTPS |
| A2 | Login y lista de rutinas ya eran compatibles con el backend | Se reutilizaron |
| A3 | El video iba a Firebase Storage y no se avisaba al backend | Era el eslabón que faltaba: el flujo del paciente nunca había cerrado |
| A4 | La cámara era una pestaña aparte, sin saber a qué rutina ni ejercicio pertenecía el video | Se le pasan `scheduled_training_id` y `exercise_id` |
| A5 | `AuthAuthenticator` repetía un 401 con el mismo token, y los interceptores escribían el token en el log | Con el vencimiento de 12 h del backend, el 401 es normal: lleva al login |
| A6 | Modelos con campos no nulos que el backend puede mandar nulos | Riesgo de cierres inesperados |
| A7 | Plugin `google-services` con el `google-services.json` del Firebase original y `tasks-genai` en `latest.release` | Se quitó Firebase y la dependencia sin versión fija |
| A8 | Un documento personal de un tercero dentro de `app/` | No se copió |
| A9 | Grabación sin límite de calidad ni duración | Un video largo pasaba los 50 MB del servicio |

## Decisiones

| # | Decisión | Por qué |
|---|---|---|
| DM1 | Copia propia de la app, sin `google-services.json` ni el documento personal | Igual que el backend y el panel: sin acceso al Firebase original y sin datos de terceros |
| DM2 | Fuera Firebase. El video va a **Supabase** con el mismo mecanismo del panel: URL firmada emitida por el backend + `PUT` directo | Un solo almacenamiento y un solo camino hacia el servicio para todos los videos |
| DM3 | **Permisos cerrados por defecto** en el backend: Admin y Therapist en lo del panel; el paciente solo en una lista explícita de endpoints y **solo sobre sus propios datos** | El backend original dejaba todo abierto; con pacientes que inician sesión, eso exponía terapias, rutinas y ejercicios |
| DM4 | Sin contraseña por defecto: al crear un usuario se genera una **contraseña temporal aleatoria** que el panel muestra una sola vez; la app ofrece "Cambiar contraseña" | El backend original creaba todos los usuarios con la misma contraseña |
| DM5 | La app solo deja entrar al grupo Patient | Los terapeutas y administradores usan el panel |
| DM6 | Grabación en 720p con tope de 60 s y verificación de tamaño antes de enviar (≤ 50 MB). Si pasa, avisa y no envía | El límite del servicio es 50 MB por video |
| DM7 | La detección en vivo queda como **guía**, rotulada "Guía en vivo. La medición la hace el servicio." | Referencia y paciente se miden con el mismo motor, como en el panel |
| DM8 | Videos de prueba sin pacientes reales (bucket de lectura pública, Ley 1581) | Un bucket privado con URL firmadas de lectura queda como mejora recomendada |

## Flujo del paciente (contrato con el backend)

```
1. POST /auth/users/login/             → token (la app solo acepta el grupo Patient)
2. GET  /scheduled_training            → sus rutinas programadas, con ejercicios
3. GET  /scheduled_training/diffs?scheduled_training_id=&exercise_id=
                                       → rango esperado, si el ejercicio tiene dificultad
4. El paciente elige un ejercicio → cámara → graba → revisa → "Enviar"
5. POST /training {schedule_training_id}
                                       → id de la sesión (la crea o la reutiliza)
6. POST /storage/upload_url (kind=result)
                                       → URL firmada de Supabase para exercise_results/<uuid>.mp4
7. PUT  <URL firmada>                  → el video, con barra de progreso
8. POST /exercise_results {training_id, exercise_id, video}
                                       → el backend lo envía al servicio
9. GET  /exercise_results?training_id= → estado por ejercicio y mínimo/máximo por articulación
10. POST /training/end?training_id=    → "Terminar rutina"
```

El backend valida en cada llamada que la sesión, la rutina o el resultado
pertenezcan al paciente; si no, responde 404, sin revelar que existe.

**Decisiones del envío:**
- **Reintentos de la subida:** cada intento pide una URL firmada nueva. Supabase
  no permite sobrescribir, así que repetir un `PUT` cuya respuesta se perdió
  fallaría con la misma URL.
- **Reintentos del aviso final:** `POST /exercise_results` se repite solo si la
  petición no llegó a salir. Si se cortó esperando la respuesta, pudo quedar
  registrado, y repetirlo duplicaría el resultado.
- **"Reintentar" sigue desde el paso pendiente**, sin crear otra sesión.
- **El `PUT` va con un cliente HTTP propio**, sin el interceptor ni el log: el
  token de la plataforma no viaja a Supabase y la URL firmada, que es una
  credencial, no queda en el log.
- **Cómo sabe la app el id de la sesión:** el paciente no puede consultar
  `GET /training`, que es del panel, y la rutina programada no trae la sesión.
  La app la pide con `POST /training`, que devuelve la existente, y solo lo hace
  si la rutina ya está "Iniciado" o "Finalizado": con "Sin iniciar" no hay
  resultados, y llamarlo crearía una sesión solo por mirar.
- **Cada rechazo tiene su mensaje** (sin red, rutina inexistente, servicio sin
  configurar, tamaño, formato o el motivo del backend); los textos están en
  `ExerciseVideoSender.kt`.

## Grabación

- **720p con alternativa** si la cámara no la ofrece, sin audio, a ~4 Mbps, en
  un archivo privado de la app. Con el bitrate por defecto de algunos teléfonos,
  un minuto pasaba de 50 MB y la grabación se habría cortado antes del minuto.
  Medido en el teléfono de prueba (Samsung A34): 30 s ocupan 14,4 MB.
- **Tope de 60 s y de 50 MB**, con el contador "0:12 / 1:00" mientras graba.
- **Revisión antes de enviar:** el video con su duración y tamaño, y "Repetir" o
  "Enviar". Si pasa de 50 MB lo dice y no deja enviarlo.
- **La grabación no queda en el teléfono:** se borra al enviarla, al repetir y al
  salir sin enviar. Un video elegido de la galería no se borra, porque no es de
  la app.

## Estado y resultado

- **Estado por ejercicio:** "Pendiente", "Procesando", "Procesado" o "Error",
  desde `GET /exercise_results?training_id=`; si un ejercicio se envió varias
  veces, vale el último envío.
- **Actualización:** al deslizar, al volver a la pantalla y cada 10 s mientras
  algún ejercicio está procesando (en segundo plano no consulta). Las consultas
  van de a una: deslizar espera a la consulta en curso y el sondeo se salta su
  turno si hay otra, para que una respuesta vieja no pise una nueva. Un fallo
  pasajero del sondeo (sin red, demora, 5xx) no se muestra hasta repetirse 3
  veces seguidas; al deslizar, el fallo sí se muestra.
- **Copia local (Room):** la lista de rutinas, los rangos esperados y el último
  estado de cada ejercicio se guardan en el teléfono. Al abrir un detalle se
  muestra lo guardado de inmediato y se actualiza en segundo plano; sin red,
  todo sigue a la vista. La copia se borra al cerrar sesión, para que otro
  paciente en el mismo teléfono no vea la del anterior.
- **"Hacer ejercicio"** aparece solo en rutinas de hoy o vencidas sin terminar,
  con el ejercicio pendiente o en error. En una rutina futura se ve el detalle y,
  en lugar del botón, "Disponible desde el viernes 30 de octubre." (con el año,
  si no es el actual). El día se calcula en un solo lugar (`scheduledDate`), en
  la zona del teléfono, para que la lista y el detalle no se contradigan.
- **"Terminar rutina"** se activa cuando todos los ejercicios tienen al menos un
  resultado, procesado o en error; la confirmación nombra los que quedaron en
  error ("Quedó en error: … Tu terapeuta lo verá.").

## Criterio de rango

Los extremos de una dificultad son **metas a alcanzar**, no una zona permitida:
el contador de repeticiones original cuenta una repetición cuando el ángulo
llega al máximo y vuelve al mínimo. Con un criterio de contención, moverse menos
salía "dentro del rango".

- **Por articulación, con la tolerancia de 5°** de la tesis (umbral de error
  angular aceptable): alcanzó si `mínimo medido ≤ mínimo esperado + 5` y
  `máximo medido ≥ máximo esperado − 5`.
- **Moverse más que la meta también la alcanza.**
- **Rótulos:** "Alcanzó el rango" (verde) o "No alcanzó el rango" (ámbar), con
  lo que faltó, primero el mínimo y después el máximo: "Llegó a 54°, meta 30°
  (faltaron 24°)". Lo que faltó se cuenta hasta la meta, no hasta la tolerancia.
- **Grados enteros:** se compara en grados enteros, como se muestran.
- **Extremo sin medir:** no se da por alcanzado ("Sin medición del máximo, meta
  120°"). Sin dificultad asignada: "Sin rango esperado para comparar."

## Seguridad

- **Sesión:** un único lugar para el token (`SessionManager`). Ante un 401, la
  app borra la sesión y la copia local y vuelve al login con "Tu sesión expiró.
  Inicia sesión de nuevo." (el token del backend vence a las 12 h). No hay
  reintento automático con el mismo token.
- **Cerrar sesión** borra la sesión local de inmediato y revoca el token con
  `POST /auth/users/logout/`; sin red, la sesión local se borra igual y el token
  vence solo.
- **Solo pacientes:** si el usuario no está en el grupo Patient, no se guarda la
  sesión y se le indica que use el panel web.
- **Logs:** sin token, `Authorization`, contraseña, usuario, correo ni URL
  firmada. `HttpLoggingInterceptor` registra solo método, URL y código, y solo en
  depuración.
- **Red:** `HOST_URL` en HTTPS (`BuildConfig`), sin tráfico en claro
  (`usesCleartextTraffic="false"`).
- **Copia de seguridad de Android desactivada:** el token y las rutinas no salen
  del teléfono.
- **Sin Firebase ni credenciales en el repositorio.**

## Cómo compilar

1. **JDK 17 a 21.** Gradle 8.9 no corre sobre JDK 23; sirve el JDK que trae
   Android Studio, solo para el comando.
2. **`local.properties`** en la raíz, con la ruta del SDK de Android. Es de cada
   equipo y no se versiona; Android Studio lo crea al abrir el proyecto:

   ```
   sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
   ```

3. **Comando** (Git Bash, desde la raíz):

   ```
   JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest assembleDebug
   ```

   La APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
4. **Al cambiar `HOST_URL`, compilar con `clean`:** la compilación incremental
   no siempre vuelve a compilar `Constants.kt`, y la APK puede quedar con la URL
   anterior.

No hace falta `google-services.json` ni ningún archivo de Firebase. MediaPipe
Tasks no trae biblioteca para x86_64, así que la app se compila para ARM
(`abiFilters`); un emulador x86_64 la ejecuta traduciendo arm64.

## Convenciones

- Identificadores en inglés; comentarios en español (el equipo sustenta en
  español).
- Todo cambio se verifica con las pruebas JVM (`testDebugUnitTest`), con el
  backend y Supabase simulados (MockWebServer); nunca se llaman los reales.
- La prueba de punta a punta se hace en un teléfono real contra Render (fase
  M5).
