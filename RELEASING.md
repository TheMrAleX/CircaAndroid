# Publicar Aproximado

`release.yml` se ejecuta al enviar un tag `v*`. Requiere que el tag coincida con `versionName`. La versión inicial es 1.0.0 (código 1). Ejecuta validación de las 430 preguntas, tests, lint release, R8 y reducción de recursos. Publica `Aproximado-v1.0.0-arm64-v8a.apk` firmado y su archivo SHA256 en GitHub Releases con notas generadas.

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

## Disparar la primera release (tres líneas)

Desde el checkout ya commiteado en main, con Git autenticado:

```sh
git push -u origin main
git tag v1.0.0
git push origin v1.0.0
```

Si el tag ya existe localmente, omite la segunda línea. Comprueba el resultado en Actions → Signed ARM64 release y en Releases. Con gh autenticado: `gh run list --workflow release.yml` y `gh release view v1.0.0`. El workflow usa `github.token` con `contents: write`; no requiere un PAT personal.

## ARMv8

`ndk.abiFilters` incluye solo `arm64-v8a`. La app actual es Kotlin/DEX y no lleva bibliotecas nativas; este filtro se aplicará a cualquier dependencia nativa futura. El APK universal de bytecode se etiqueta arm64-v8a como distribución solicitada; Android puede ejecutarlo también en otras arquitecturas compatibles porque no depende de código JNI.
