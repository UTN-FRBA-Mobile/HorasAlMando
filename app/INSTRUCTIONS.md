# Instrucciones de Desarrollo - HorasAlMando

## Contexto
Proyecto de desarrollo Android (Kotlin/Jetpack Compose) enfocado en una aplicación de gestión de vuelos. El código debe priorizar la estabilidad, la legibilidad y la arquitectura orientada a la persistencia local (offline-first).

## Stack Tecnológico
- Lenguaje: Kotlin
- UI: Jetpack Compose
- Arquitectura: MVVM (Model-View-ViewModel)
- Persistencia: Room / Gestión de estados locales
- Librerías clave: WorkManager (para tareas en background), Google Maps API

## Convenciones de Estilo
- Seguir estándares de Google Kotlin Style Guide.
- Usar `camelCase` para variables y funciones; `PascalCase` para clases y Composables.
- Mantener los Composables pequeños y reutilizables. La lógica de negocio debe residir exclusivamente en el ViewModel.

## Reglas de Arquitectura
- Prioridad "Offline-First": Toda operación debe intentar ejecutarse localmente antes de sincronizar con el servidor.
- Uso de `WorkManager` para toda subida de datos automática en background cuando la red esté disponible.
- Evitar lógica de estado (business logic) dentro de los bloques `Composable`.

## Preferencias de UI/UX
- Notificaciones: Utilizar `Snackbar` para feedback al usuario por defecto. Los `Toast` solo se usarán si el sistema no permite `Snackbar`.
- Animaciones: Las notificaciones de subida deben ser sutiles, deslizándose desde la parte superior.
- Consistencia en el Mapa: Marcadores de waypoints deben ser consistentes con el estilo visual (aviones naranjas en modo ghost).

## Instrucciones para el AI Assistant
- Antes de modificar UI, verifica que el estado esté correctamente definido en el ViewModel.
- Si el "Project context" está deshabilitado, solicita explícitamente los archivos necesarios (`@filename`) antes de realizar refactorizaciones importantes.