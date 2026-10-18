# Publicar Aproximado

`release.yml` se ejecuta al enviar un tag `v*`. Requiere que el tag coincida con `versionName`. La versión actual es 1.0.6 (código 7). Ejecuta validación de las 400 preguntas, tests, lint release, R8 y reducción de recursos. Publica `Aproximado-v1.0.6-arm64-v8a.apk` firmado y su archivo SHA256 en GitHub Releases con notas generadas.

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
git tag v1.0.6
git push origin v1.0.6
```

Si el tag ya existe localmente, omite la segunda línea. Comprueba el resultado en Actions → Signed ARM64 release y en Releases. Con gh autenticado: `gh run list --workflow release.yml` y `gh release view v1.0.6`. El workflow usa `github.token` con `contents: write`; no requiere un PAT personal.

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

## Publicación verificada: v1.0.2

Publicada el 18 de octubre de 2026 desde `0ff34b6` (versionCode 3):

- [Release v1.0.2](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.2)
- [APK ARM64 firmado](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.2/Aproximado-v1.0.2-arm64-v8a.apk)
- [CI correcto](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39090848418)
- [Release correcta](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39090914423)

El APK oficial descargado verifica firma v2, ARM64 exclusivo, versión 1.0.2/código 3, 430 preguntas, integridad SQLite y ausencia de INTERNET. SHA256:

```text
a01186cdca1428fb36bc84aad163937593c5b3727ac4969d97259054fb29e732
```

Certificado SHA256: `77dde14adbea29eee3916cd2496a400cfff746d6bad3f9352a9ef8c2f8c000d6`. Es distinto del de v1.0.1: la firma temporal vuelve a requerir desinstalar la versión anterior, con pérdida de partida, configuración e historial locales. No se dispone de la clave temporal anterior para producir una actualización compatible. Para futuras versiones, configurar una clave estable mediante las secrets indicadas al inicio.

## Publicación verificada: v1.0.3

Publicada el 18 de octubre de 2026 desde `bd071e8` (versionCode 4):

- [Release v1.0.3](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.3)
- [APK ARM64 firmado](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.3/Aproximado-v1.0.3-arm64-v8a.apk)
- [CI correcto](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39093624442)
- [Release correcta](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39093675819)

El APK oficial descargado verifica firma v2, ARM64 exclusivo, versión 1.0.3/código 4, SQLite esquema 2 íntegra con 430 preguntas (412 enteras, 6 de un decimal, 10 de dos y 2 de tres), migración SQL empaquetada y ausencia de INTERNET. La fixture v1 de los tests no se empaqueta. SHA256:

```text
7c83874ab90fb5d412665b91a28c31f1236c76087821aae1ba21a549e15fa435
```

Certificado SHA256: `67207e3529bb31bd8bca2c1de1e62f6e2a4b82c82e0e9eecdcd7de92a939c362`. Es distinto del de 1.0.2, por lo que Android requiere desinstalar la anterior, perdiendo los datos locales. La migración de Room conserva datos cuando se instala una actualización con firma compatible; no evita la restricción de firmas de Android. Para futuras versiones compatibles, usar las secrets de una clave estable descritas al inicio.

## Publicación verificada: v1.0.4

Publicada el 18 de octubre de 2026 desde `24ff8c6` (versionCode 5):

- [Release v1.0.4](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.4)
- [APK ARM64 firmado](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.4/Aproximado-v1.0.4-arm64-v8a.apk)
- [CI correcto](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39144757551)
- [Release correcta](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39144766164)

El APK oficial descargado verifica firma v2, ARM64 exclusivo, versión 1.0.4/código 5, SQLite esquema 3 íntegra con 400 preguntas activas (350 enteras, 50 con decimales), migraciones empaquetadas y ausencia de INTERNET. Las 400 filas coinciden íntegramente con los JSON finales. Las fixtures de tests no se empaquetan. SHA256:

```text
4878e463a6df6eb8aefc351aece68ce799f612b7572eeeba01a065b54098c027
```

Certificado SHA256: `a32801d393df8d610ea5f59afc766d4400e9e48b16b36adf1cf506a3bf868935`. Es diferente de 1.0.3: Android exige desinstalar la anterior y se pierden los datos locales. La migración preserva partidas cuando la actualización usa una firma compatible, pero no elimina esta restricción de Android. Configurar una clave estable mediante las secrets descritas arriba para futuras versiones compatibles.

## Publicación verificada: v1.0.5

Publicada el 18 de octubre de 2026 desde `207758c` (versionCode 6):

- [Release v1.0.5](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.5)
- [APK ARM64 firmado](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.5/Aproximado-v1.0.5-arm64-v8a.apk)
- [CI correcto](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39147800217)
- [Release correcta](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39147825478)

APK oficial descargado: versión 1.0.5/código 6, firma v2 RSA3072, ARM64 exclusivo, SQLite 3 íntegra y coincidencia completa de las 400 preguntas con los JSON. Servicio LAN no exportado con tipo connectedDevice y permisos LAN/foreground/notificación esperados; sin ubicación ni escaneo Wi-Fi. Las fixtures y keystores no se empaquetan. SHA256:

```text
c05771068822dfcaf6f3dd3e94815dab31caecfc811a3ce56fd5fdfe50ba63a6
```

Certificado SHA256: `fd0cefc39cec406fd52c9e579eff943fb2d6f9b65810e2a3dfe0d0004dc2616c`. Es diferente de 1.0.4: hay que desinstalar la anterior y se pierden los datos locales. Una firma estable mediante las secrets descritas al inicio permite actualizaciones futuras compatibles; no recupera la clave temporal anterior.

Esta versión añade INTERNET porque Android lo exige para sockets LAN; no usa servidores externos. Las comprobaciones automatizadas de sockets/Android no equivalen a probar radios Wi-Fi, hotspot ni políticas de ahorro en teléfonos físicos. Véase LAN.md.

## Publicación verificada: v1.0.6

Publicada el 18 de octubre de 2026 desde `7b0925a` (versionCode 7):

- [Release v1.0.6](https://github.com/TheMrAleX/CircaAndroid/releases/tag/v1.0.6)
- [APK ARM64 firmado](https://github.com/TheMrAleX/CircaAndroid/releases/download/v1.0.6/Aproximado-v1.0.6-arm64-v8a.apk)
- [CI correcto](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39153767334)
- [Release correcta](https://github.com/TheMrAleX/CircaAndroid/actions/runs/39153772943)

APK oficial descargado: versión 1.0.6/código 7, firma v2 RSA3072, ARM64 exclusivo, SQLite 3 íntegra y coincidencia completa de las 400 filas con los JSON. Las fixtures, keystores y animación APNG de tests no se empaquetan. SHA256:

```text
4df4d328df6337c8efefe3a79db566d47a120b80913c3ba7ab3f91316d711b55
```

Certificado SHA256: `796d4fa9761a60176edb757fb650bf86d0401b906593d19114dacc877d0be113`. Difiere del de 1.0.5: Android exige desinstalar la anterior y se pierden los datos locales. Configurar una firma estable mediante las secrets descritas al inicio para futuras actualizaciones compatibles; no recupera la clave temporal anterior.
