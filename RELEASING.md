# Publicar Aproximado

`release.yml` se ejecuta al enviar un tag `v*`. Requiere que el tag coincida con `versionName`. La versión actual es 1.0.1 (código 2). Ejecuta validación de las 430 preguntas, tests, lint release, R8 y reducción de recursos. Publica `Aproximado-v1.0.1-arm64-v8a.apk` firmado y su archivo SHA256 en GitHub Releases con notas generadas.

## Firma estable (recomendada)

Guarda en **Settings → Secrets and variables → Actions**:
- `KEYSTORE_BASE64`: contenido base64 de tu keystore.
- `KEYSTORE_PASSWORD`: contraseña del almacén.
- `KEY_ALIAS`: alias de la clave.
- `KEY_PASSWORD`: contraseña de la clave.

No pegues claves en issues, commits ni conversaciones. Conserva una copia segura del almacén. La misma firma y el mismo applicationId permiten instalar actualizaciones encima de versiones anteriores; aumenta `versionCode` en cada publicación.

Si no hay `KEYSTORE_BASE64`, el workflow genera una clave temporal con keytool y publica un APK firmado e instalable. **La clave cambia por ejecución: los APK de ejecuciones diferentes no actualizan los anteriores.** Para actualizar sin desinstalar, configura desde la primera versión una clave estable. Si existe base64 pero faltan las otras secrets, el workflow falla para no publicar una firma incorrecta.

Puedes generar y verificar un APK local con `python3 scripts/build_local_release.py`; usa las cuatro variables indicadas si hay base64 o genera una clave temporal que se borra al terminar. El resultado y su SHA256 quedan en `dist/`, ignorado por Git.

Para firmar localmente, exporta `KEYSTORE_PATH` (ruta absoluta), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` y ejecuta `./gradlew :app:assembleRelease`. No se usa la clave debug para release. No se versionan almacenes ni contraseñas.

## Disparar la release (tres líneas)

Desde el checkout ya commiteado en main, con Git autenticado:

```sh
git push -u origin main
git tag v1.0.1
git push origin v1.0.1
```

Si el tag ya existe localmente, omite la segunda línea. Comprueba el resultado en Actions → Signed ARM64 release y en Releases. Con gh autenticado: `gh run list --workflow release.yml` y `gh release view v1.0.1`. El workflow usa `github.token` con `contents: write`; no requiere un PAT personal.

## ARMv8

`ndk.abiFilters` incluye solo `arm64-v8a`. La app usa Kotlin/DEX y una biblioteca nativa transitiva de AndroidX. El APK empaqueta únicamente `lib/arm64-v8a/libandroidx.graphics.path.so`; se distribuye para dispositivos ARM64.

## Publicación verificada: v1.0.0

Publicada el 17 de octubre de 2026 desde el commit `4b5012e` (tag `v1.0.0`):

- [Release v1.0.0](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.0)
- [APK ARM64 firmado](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.0/Aproximado-v1.0.0-arm64-v8a.apk)
- [Workflow release correcto](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39070610379)
- [CI correcto](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39070609124)

Se descargaron el APK y su SHA256 desde los assets publicados y se contrastaron. `apksigner verify` pasó (firma v2). El APK contiene 430 preguntas, SQLite íntegra, solo bibliotecas nativas ARM64 y ningún permiso INTERNET. Certificado publicado: `CN=Aproximado, O=Offline Games, C=ES`. Conserva una clave estable mediante las secrets descritas arriba para futuras actualizaciones.

SHA256 del APK publicado:

```text
7fb46502e3a3437cb392b3a73ab016d7bf7c470ab2c9289c72fa5d6c6de8e6d4
```

La compilación local con firma temporal genera un SHA diferente: no se debe comparar con el hash del APK de GitHub. Usa el archivo .sha256 adjunto al mismo APK.

## Publicación verificada: v1.0.1

Publicada el 18 de octubre de 2026 desde `03267ea` (versionCode 2):

- [Release y APK](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.1)
- [CI correcto del código publicado](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39072467739)
- [Release completada correctamente](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39089680892)

El APK descargado verifica firma v2, ARM64 exclusivo, 430 preguntas, integridad SQLite y ausencia de permiso INTERNET. SHA256:

```text
3b024458f846c3efe60c4e692c211a2060dacd551e0900bd1e178d6b1e404353
```

**Instalación sobre v1.0.0:** las firmas publicadas son diferentes. Certificado SHA256 de v1.0.0: `fcf0d2fc923660af0cad63fa2b17ca6fd18103947c44e6f7b9bed73f5c47170c`; de v1.0.1: `46a0e40c09b58d21576070b49a37bb6348e2e0e82defa4d00ff6ef36e162245d`. Para instalar v1.0.1 hay que desinstalar v1.0.0, perdiendo partida e historial locales. Configurar una clave estable permite conservar compatibilidad entre las futuras versiones que usen esa clave; no recupera la clave temporal anterior.
