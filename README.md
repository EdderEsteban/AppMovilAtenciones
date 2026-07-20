# AppMovilAtenciones

Aplicación móvil nativa para Android (Java) que digitaliza el registro de atenciones de **enfermería** y **odontología** en instituciones de salud públicas, con capacidad de trabajar **completamente offline** y sincronizar cuando vuelve la conexión.

Es el cliente móvil del sistema **AtencionesApp**, una API REST en ASP.NET Core que centraliza pacientes, atenciones y estadísticas de múltiples instituciones sanitarias.

> Proyecto de tesis — Tecnicatura/Licenciatura en Desarrollo de Software.

---

## ¿Qué resuelve?

El personal de salud (enfermeros y odontólogos) que atiende en centros con conectividad intermitente o nula necesita poder:

- Buscar o dar de alta un paciente por DNI, sin depender de tener señal en el momento.
- Registrar una atención completa (enfermería u odontología, incluyendo el odontograma) offline.
- Que esos registros se suban solos al servidor apenas vuelve la conexión, sin intervención manual.
- Ver su propio historial y estadísticas de actividad por institución.

## Características principales

- **Offline-first real**: toda la app lee y escribe primero en una base de datos local (Room); la UI nunca depende de la red para mostrar información ya conocida.
- **Reconciliación por DNI**: un paciente cargado sin conexión se reconcilia automáticamente contra el servidor por su DNI al sincronizar, evitando duplicados.
- **Dos flujos clínicos completos**:
  - **Enfermería**: atención ambulatoria/internación, prestaciones, obra social dinámica.
  - **Odontología**: odontograma FDI interactivo (32 piezas permanentes + 20 temporarias), cálculo de índice CPO/ceo, diagnóstico CIE-10 buscable, prestaciones.
- **La app se adapta al rol del profesional** (Enfermero u Odontólogo) automáticamente al iniciar sesión — cada uno ve únicamente su flujo.
- **Multi-institución**: un profesional puede tener asignada más de una institución y elegir con cuál trabajar en cada momento; los registros quedan asociados a la institución activa al capturarlos.
- **Sincronización resiliente**: distingue errores transitorios (sin conexión, error de servidor → reintenta solo) de errores permanentes de negocio (dato inválido → se marca como error y no vuelve a reintentar indefinidamente).
- **Sesión con JWT** contra la API .NET, con selección de institución posterior al login.

## Capturas

*(agregar capturas de pantalla acá — Login, Inicio, Ficha de paciente, Registrar atención, Odontograma)*

## Arquitectura

- **Patrón**: MVVM, sin capa de Repository — los `ViewModel` hablan directo con los DAO de Room y con `ApiClient` (Retrofit). Toda la lógica de negocio vive en los ViewModels; las `Activity` solo inflan vistas y observan `LiveData`.
- **Persistencia**: Room como única fuente de verdad para la UI. Cada entidad sincronizable tiene `localId` (clave local autogenerada), `serverId` (nulo hasta sincronizar) y `syncState` (`PENDIENTE` / `SINCRONIZADO` / `ERROR`).
- **Red**: Retrofit2 + Gson, con `@Header("Authorization")` explícito en cada llamada (sin interceptor global). Token JWT persistido en `SharedPreferences`.
- **Sincronización**: dispara en primer plano (al abrir la pantalla de Sincronización o al recuperar conexión), corriendo en un `ExecutorService` de fondo — deliberadamente **sin WorkManager** ni tareas en segundo plano.
- **UI**: ViewBinding (sin `findViewById`), Material Components 3, `RecyclerView` con adapters estándar.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java |
| UI | Android Views + ViewBinding + Material Components 3 |
| Persistencia local | Room (SQLite) |
| Red | Retrofit2 + Gson + OkHttp |
| Concurrencia | `ExecutorService` + `Handler` (sin coroutines/RxJava) |
| Arquitectura | MVVM (`AndroidViewModel` + `LiveData`) |
| Backend consumido | ASP.NET Core 8 + JWT Bearer (repo [`AtencionesApp`](../AtencionesApp)) |
| Min SDK / Target SDK | 28 / 36 |

## Estructura del proyecto

```
app/src/main/java/com/example/registrosatenciones/
├── db/                     # Room: entidades, DAOs, relaciones, AppDatabase
├── request/  response/     # DTOs de Retrofit (request/response) + ApiClient
├── adapters/                # RecyclerView.Adapter de cada listado
├── util/                    # PreferenciasUsuario, AppExecutors, Conectividad, CatalogoSync
└── ui/
    ├── login/  seleccioninstitucion/  perfil/
    ├── pacientes/  altapaciente/  fichapaciente/
    ├── registraratencion/               # Enfermería
    ├── registraratencionodontologia/    # Odontología
    ├── odontologia/                     # Modelo del odontograma + editor de cuadrante
    ├── detalleatencionodontologia/
    ├── sincronizacion/  inicio/  common/
```

## Requisitos

- Android Studio (Ladybug o superior recomendado).
- JDK 11+ (se usa el JBR embebido de Android Studio).
- Un dispositivo/emulador con Android 9 (API 28) o superior.
- La API [`AtencionesApp`](../AtencionesApp) corriendo y accesible desde el dispositivo (ver ese repo para levantarla localmente).

## Cómo compilar y ejecutar

```bash
git clone <url-del-repo>
cd AppMovilAtenciones
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para apuntar la app a tu propia instancia del backend, editá la URL base en:

```java
// app/src/main/java/com/example/registrosatenciones/request/ApiClient.java
public static final String URLBASE = "https://tu-backend/api/";
```

> Si probás contra un emulador con el backend corriendo en tu misma máquina, usá `http://10.0.2.2:<puerto>/api/` (alias especial del emulador hacia `localhost` del host). En un dispositivo físico necesitás que el backend sea alcanzable en la red (por ejemplo, con un túnel como VS Code devtunnels o ngrok).

## Roles y usuarios de prueba

El rol viene del backend (claim del JWT) y determina qué flujo ve la app:

- **Enfermero** → módulo de enfermería.
- **Odontólogo** (con tilde, tal como lo emite el backend) → módulo de odontología.

## Estado del proyecto

MVP funcional y verificado de punta a punta (offline → sincronización → verificación contra el backend real) para ambos módulos clínicos.

## Licencia

Proyecto académico, desarrollado como parte de una tesis de grado. Sin licencia de uso comercial definida.
