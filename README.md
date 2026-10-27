# TherApp

Aplicación Android de **telerehabilitación** que utiliza detección de poses en tiempo real mediante **MediaPipe** para guiar y monitorear ejercicios de fisioterapia. Permite a los pacientes realizar rutinas asignadas mientras la app rastrea articulaciones, calcula ángulos y cuenta repeticiones.

## Características

- **Detección de poses en tiempo real** con MediaPipe Pose Landmarker (modelos lite, full y heavy)
- **Cálculo de ángulos articulares** y conteo automático de repeticiones
- **Grabación de video** de sesiones de ejercicio con CameraX
- **Envío de videos** a la plataforma con seguimiento de progreso (URL firmada que emite el backend)
- **Gestión de rutinas y terapias**: visualización de entrenamientos programados, detalle de ejercicios con dificultades y rangos de movimiento
- **Sesión con token** del backend: si vence, la app vuelve al inicio de sesión
- **Caché local** con Room para funcionamiento offline
- **Selector de articulaciones** configurable para rastrear puntos específicos del cuerpo
- **Controles de cámara**: zoom, flash, cambio de cámara frontal/trasera

## Arquitectura

El proyecto sigue **Clean Architecture** con patrón **MVVM**:

```
app/src/main/java/com/example/therapp/
├── app/                  # Entry point de la app (PoseLandMarkerApp)
├── common/               # Constantes, DataStore, utilidades de API
├── data/                 # Capa de datos
│   ├── auth/             # Repositorio e API de autenticación
│   ├── exercise/         # Envío del video: URL firmada, subida y aviso al backend
│   ├── local/            # Room Database, DAOs
│   ├── results/          # Estado y resultados de la sesión
│   └── routines/         # Repositorio, API, entidades y mappers de rutinas
├── di/                   # Módulos de Dagger Hilt (API, DB, Repository, UseCases)
├── domain/               # Capa de dominio
│   ├── model/            # Modelos de negocio (Routine, Exercise, Therapy, etc.)
│   ├── repository/       # Interfaces de repositorios
│   └── use_cases/        # Casos de uso (auth, routines)
├── security/             # Sesión, token y acceso solo del grupo Paciente
└── ui/                   # Capa de presentación
    ├── components/       # Componentes reutilizables y sistema de pose overlay
    │   └── pose/         # Renderers, calculators, strategies de pose
    ├── global_viewmodels/# AuthViewModel
    ├── navigation/       # Grafos y rutas de navegación
    ├── presenter/        # Pantallas y ViewModels
    │   ├── home/         # Pantalla principal con bottom/drawer navigation
    │   ├── pose_camera/  # Cámara con detección de poses
    │   ├── routines/     # Listado y detalle de rutinas
    │   ├── sign_in/      # Inicio de sesión
    │   ├── splash/       # Splash screen
    │   └── therapies/    # Pantalla de terapias
    └── theme/            # Colores, tipografía, tema Material 3
```

## Stack Tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Inyección de dependencias**: Dagger Hilt + KSP
- **Networking**: Retrofit + OkHttp
- **Base de datos local**: Room
- **Preferencias**: DataStore
- **Cámara**: CameraX
- **Detección de poses**: MediaPipe Pose Landmarker
- **Almacenamiento de videos**: el de la plataforma, con la URL firmada que emite el backend
- **Navegación**: Jetpack Navigation (type-safe con Kotlinx Serialization)
- **Reproducción de video**: Media3 / ExoPlayer
- **Carga de imágenes**: Glide Compose

## Requisitos

- Android Studio Hedgehog o superior
- JDK 17 a 21 para Gradle (el código compila para Java 11)
- Android SDK 35 (compilación)
- Dispositivo o emulador con API 24+ (Android 7.0)

## Configuración

1. Clonar el repositorio:
   ```bash
   git clone <url-del-repositorio>
   ```

2. Abrir el proyecto en Android Studio.

3. La URL del backend es `HOST_URL`, en `app/build.gradle.kts` (solo HTTPS).
   La app no usa Firebase ni necesita `google-services.json`.

4. Sincronizar Gradle y ejecutar la app. Los detalles (JDK, compilar desde la
   consola, cambiar `HOST_URL`) están en `docs/DECISIONES_TECNICAS.md`,
   "Cómo compilar".

## Autor

**Santiago Varela Daza**
- Email: svarela03@uan.edu.co
- GitHub: [sanvarela03](https://github.com/sanvarela03)

---

## Origen de esta copia

Copia propia de [`santivarelaagent-cmd/therapp`](https://github.com/santivarelaagent-cmd/therapp)
(commit `e3623fb`, 2026-03-06), hecha el 2026-10-19 para integrar la app con la copia de la
plataforma de telefisioterapia y el Servicio de Procesamiento del trabajo de grado (Universidad
Antonio Nariño, grupo LACSER/Bioingeniería). Los planes de trabajo de la plataforma están
resumidos en `docs/DECISIONES_TECNICAS.md` de
[`telefisioterapia-backend`](https://github.com/davidgarcia05042-netizen/telefisioterapia-backend).
Las decisiones de diseño, el contrato del flujo del paciente y cómo compilar están en
[`docs/DECISIONES_TECNICAS.md`](docs/DECISIONES_TECNICAS.md).
Se excluyeron `app/google-services.json` (configuración del Firebase original) y un documento
personal que no pertenecía al proyecto.

**Repositorios relacionados:**

| Repositorio | Qué es |
|---|---|
| [`telefisioterapia-backend`](https://github.com/davidgarcia05042-netizen/telefisioterapia-backend) | Backend de la plataforma (Django + DRF), al que se conecta la app |
| [`telefisioterapia-panel`](https://github.com/davidgarcia05042-netizen/telefisioterapia-panel) | Panel web del terapeuta |
| [`servicio-procesamiento-pose`](https://github.com/davidgarcia05042-netizen/servicio-procesamiento-pose) | Servicio de Procesamiento: mide los videos que envía la app |
| [`entorno-pruebas`](https://github.com/davidgarcia05042-netizen/entorno-pruebas) | Comparación de modelos de pose contra gold standard de laboratorio |
| [`pose-benchmark`](https://github.com/davidgarcia05042-netizen/pose-benchmark) | Tiempo, CPU y memoria de cada modelo |
