# Pruebas de reglas de Firestore

Esta suite usa exclusivamente el Firestore Emulator en `127.0.0.1:8080` y
autenticación simulada mediante `@firebase/rules-unit-testing`.

Con el emulador iniciado desde la raíz del proyecto:

```powershell
cd firestore-rules-tests
npm install
npm test
```

La suite carga `../firestore.rules`, limpia únicamente el proyecto emulado
`embarba-rutas` y crea sus propios datos ficticios antes de cada prueba.
