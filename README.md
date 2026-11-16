
# Embarba Rutas

**Aplicación móvil para la gestión de rutas del personal de supervisión de Embarba Elevadores S.A.**

Proyecto de investigación académica desarrollado en 2026.

## Descripción del proyecto

Embarba Rutas es una aplicación móvil Android desarrollada para apoyar la gestión de rutas del personal de supervisión de Embarba Elevadores S.A.

La aplicación permite consultar las supervisiones asignadas, visualizar la ubicación de los establecimientos, acceder a rutas de navegación, registrar la llegada mediante geolocalización GPS y documentar las actividades realizadas durante cada supervisión.

Asimismo, facilita el seguimiento del estado de las supervisiones, la consulta del historial y la generación de reportes en formato PDF.

## Objetivo

Desarrollar una aplicación móvil que contribuya a mejorar la gestión de rutas del personal de supervisión mediante la organización de supervisiones, la consulta de destinos y el seguimiento de las actividades realizadas.

## Funcionalidades implementadas

1. Inicio de sesión mediante Firebase Authentication.
2. Visualización de supervisiones asignadas.
3. Consulta de información y estado de cada supervisión.
4. Visualización de ubicaciones mediante Google Maps.
5. Navegación hacia los establecimientos asignados.
6. Registro de llegada mediante geolocalización GPS.
7. Registro de listas de verificación (checklist).
8. Registro de información de movilidad.
9. Finalización de supervisiones.
10. Consulta del historial de supervisiones finalizadas.
11. Generación de reportes de supervisión en formato PDF.

## Tecnologías utilizadas

- **Lenguaje:** Kotlin.
- **Entorno de desarrollo:** Android Studio.
- **Interfaz de usuario:** Jetpack Compose.
- **Autenticación:** Firebase Authentication.
- **Base de datos:** Cloud Firestore.
- **Geolocalización:** Servicios de ubicación de Android.
- **Navegación:** Google Maps.
- **Control de versiones:** Git y GitHub.

## Descarga de la aplicación

La versión 1.0.0 de Embarba Rutas se encuentra disponible para su descarga en GitHub Releases.

**[Descargar Embarba Rutas v1.0.0](https://github.com/docanaml-rgb/EmbarbaRutas/releases/tag/v1.0.0)**

### Instrucciones de instalación

1. Acceder al enlace de descarga.
2. Descargar el archivo `app-debug.apk` desde la sección Assets.
3. Instalar el archivo APK en un dispositivo Android compatible.
4. Abrir la aplicación e iniciar sesión con una cuenta de prueba autorizada.

**Nota:** La aplicación requiere conexión a Internet para utilizar los servicios de Firebase. Algunas funciones requieren permisos de ubicación y disponibilidad de Google Maps.

## Estructura del proyecto

- `app/`: código fuente, interfaces, modelos y recursos de la aplicación Android.
- `gradle/`: archivos de configuración de Gradle.
- `firestore.rules`: reglas de seguridad de Cloud Firestore.
- `firestore-rules-tests/`: pruebas de las reglas de seguridad.
- `firebase.json`: configuración del proyecto Firebase.

## Alcance académico

Embarba Rutas fue desarrollada como parte de un proyecto de investigación académica orientado a evaluar la gestión de rutas del personal de supervisión.

La aplicación fue implementada y probada en un entorno de desarrollo Android utilizando un emulador, Firebase y registros de prueba.

Esta versión se distribuye con fines académicos y de evaluación. No representa un despliegue comercial en producción.

## Repositorio

Código fuente disponible en:

https://github.com/docanaml-rgb/EmbarbaRutas
