# Historia clínica completa (todos los centros) con conexión

**Fecha:** 2026-07-21
**Módulo:** app móvil (AppMovilAtenciones) — integrador Lab III

## Problema

Hoy la ficha del paciente en la app muestra la historia clínica (HC) **solo de lo
local** (Room): lo que se cargó en este dispositivo o bajó la sincronización de la
institución activa. Si el paciente se atendió en otro centro de salud, esas atenciones
no se ven. Además, el timeline de la ficha está separado por rol (enfermería o
odontología según el usuario).

## Objetivo

Cuando el dispositivo **tiene internet**, la ficha debe mostrar **todas las atenciones
del paciente, de todos los centros** (enfermería + odontología), y permitir abrir el
**detalle completo de cada una, incluido el odontograma** de las atenciones
odontológicas. **Sin internet**, se mantiene el comportamiento actual (local).

## Alcance

**Incluye:**
- Ficha en "modo online": timeline unificado (enf + odo) de todos los centros.
- Detalle online de cada atención (odontológica con odontograma; de enfermería).
- Fallback automático a lo local sin conexión o ante error de red.

**No incluye (fuera de alcance de esta versión):**
- Cachear en Room la HC de otros centros para verla offline.
- Editar atenciones de otros centros (la app no edita atenciones; todo es solo lectura).
- Mostrar el centro de cada atención en el **listado** (el DTO del listado no lo trae;
  sí aparece en el detalle). Agregarlo al listado tocaría el server.

## Decisión de arquitectura: consulta directa (read-through)

Con conexión, la app consulta el server en el momento y renderiza en **solo lectura**,
**sin persistir** esos datos en Room. Se descarta el enfoque de descargar/cachear la HC
ajena en la base local por su costo y riesgo (mezclar registros ajenos con los propios,
evitar re-subidas en la sync, deduplicar con pendientes) cerca de la fecha de entrega.

El server **ya expone** todo lo necesario, sin scoping por institución:
- `GET /api/pacientes/{id}` → ficha + HC completa (enf + odo), timeline ordenado.
- `GET /api/atenciones-odontologia/{id}` → detalle con **odontograma**, valoración CPO,
  prestaciones, observaciones, institución y profesional.
- `GET /api/atenciones-enfermeria/{id}` → detalle de enfermería.

Los métodos Retrofit correspondientes ya existen en `ApiClient` (`obtenerPaciente`,
`obtenerAtencionOdontologia`, `obtenerAtencionEnfermeria`). El trabajo es del lado de la
app.

## Comportamiento

### Decisión de modo (al abrir la ficha)
- **Online + paciente con `serverId`** → modo online (HC completa del server).
- **Offline, o paciente sin `serverId`** (creado offline, aún sin sincronizar) → modo
  local (Room), como hoy.
- Se usa el util `Conectividad` existente. Un rótulo indica el modo:
  - Online: "Historia clínica completa · todos los centros".
  - Local: "Sin conexión · registros de este dispositivo".

### Ficha en modo online
- Timeline **unificado** de enfermería + odontología (el server los devuelve juntos y
  ordenados). Cada ítem con distintivo de rama (Enfermería / Odontología), fecha,
  resumen y prestaciones.
- **Sin importar el rol**, se ven ambas ramas (es la HC completa). El botón "Nueva
  atención" **sigue siendo según el rol** (cada usuario carga solo en su módulo).
- **Pendientes locales:** las atenciones cargadas en este equipo que todavía no
  sincronizaron se **mezclan al inicio del listado**, para que no "desaparezcan" hasta
  que la sync las suba.

### Detalle en modo online
- Ítem odontológico → `GET /api/atenciones-odontologia/{id}`: se dibuja el odontograma
  (mapeando los estados del server a `OdontogramaItem` → `OdontogramaView`) + CPO +
  prestaciones + observaciones + centro y profesional.
- Ítem de enfermería → `GET /api/atenciones-enfermeria/{id}`: tipo, prestaciones,
  observaciones + centro y profesional.
- Solo lectura.

## Componentes afectados

- **`FichaPacienteActivity` / `FichaPacienteViewModel`**: deciden modo (online/local),
  cargan el timeline online (server + merge de pendientes locales) o el local actual, y
  exponen el estado (cargando / online / local / error).
- **Adapter de timeline unificado (nuevo)**: renderiza ítems `AtencionResumenResponse`
  (tipo "E"/"O"), con click que rutea al detalle por (tipo, `serverId`).
- **Pantallas de detalle** (`DetalleAtencionOdontologiaActivity`,
  `DetalleAtencionEnfermeriaActivity`): se les agrega una **fuente online** (por
  `serverId`, vía API) además de la local (por `localId`, vía Room). Layouts reutilizados;
  se muestran centro/profesional cuando vienen.
- **Mapper (nuevo, lógica pura)**: `OdontogramaEstadoResponse` → `OdontogramaItem`.
- **Lógica de decisión de modo (pura)**: (hayInternet, tieneServerId) → modo.
- **`ApiClient`** y `PreferenciasUsuario` (token JWT): ya existentes.

## Flujo de datos

1. Búsqueda de paciente (ya server-backed cuando hay internet) → `serverId` disponible.
2. Ficha online → `obtenerPaciente(serverId)` → timeline unificado + merge de pendientes
   locales → adapter.
3. Tap en atención → detalle online → `obtenerAtencion*(serverId)` → render (odontograma
   desde estados, etc.).

## Manejo de errores

- Sin internet → ficha local (comportamiento actual).
- Con internet pero la consulta falla (timeout, token vencido, 5xx) → **fallback a lo
  local** + aviso discreto ("No se pudo traer la HC completa, mostrando lo local").
- Spinner mientras carga.
- Paciente sin `serverId` → local (no se puede consultar el server).

## Testing

- **Unit (puro):** mapper `OdontogramaEstadoResponse` → `OdontogramaItem`; lógica de
  decisión de modo.
- **Manual (emulador/celular):** ficha online con paciente de varios centros, apertura de
  detalle con odontograma de otro centro, y fallback offline / error de red.

## Limitaciones conocidas / futuro

- No se ve la HC de otros centros sin internet (no se cachea). Posible mejora futura
  (Enfoque 2: descarga a Room).
- El centro no aparece en el listado, solo en el detalle (requiere cambio en el DTO del
  server).
