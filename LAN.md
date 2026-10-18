# Jugar por Wi-Fi o hotspot (1.0.8)

Se necesitan de 2 a 8 teléfonos Android con Aproximado 1.0.8, uno por persona. El anfitrión también juega. El número de clientes que admite un hotspot depende del teléfono; si permite menos, usa una Wi-Fi compartida. No se necesitan Internet, datos móviles, cuentas ni servidores externos. El modo de pasar un solo teléfono sigue disponible en **Jugar**.

## Misma Wi-Fi

1. Conecta todos los teléfonos a la misma Wi-Fi.
2. En un teléfono: **Jugar en red local → Tu nombre → Crear sala**.
3. Comparte el código de ocho dígitos y la **IP:puerto** que aparecen. Si hay varias direcciones, usa la de la red compartida.
4. En los demás: **Jugar en red local → Tu nombre → Unirse a una sala**. Toca una sala detectada o escribe su IP:puerto y después el código. Toca **Conectar**.
5. El anfitrión ve quién está conectado, elige rondas/categorías/dificultad e inicia. Todos responden simultáneamente desde su teléfono. La respuesta real se revela cuando todos confirman. Con «Regla en vivo» habilitada por todos se ven las barras de los 2–8 jugadores; sin consentimiento unánime, las estimaciones son privadas. El anfitrión continúa al marcador y a la siguiente ronda.

## Regla en vivo (1.0.8)

Con **2–8 jugadores**, si todos tienen **Ajustes → Regla en vivo** activada y están conectados, el selector muestra un único gráfico con todas las barras, incluida la tuya. Tu nombre y color aparecen arriba. Arrastra verticalmente sobre cualquier columna, el fondo, la leyenda o el área de controles para ajustar **solo tu estimación**. Conservas los botones +/− y el teclado.

Las columnas mantienen el mismo orden y escala de la pregunta, para no exagerar diferencias ni cambiar de sitio mientras razonan. Los valores están en una leyenda fija de una o dos filas; el nombre completo y la cifra precisa permanecen accesibles. Al confirmar, tu barra queda congelada con ✓ y puedes observar las demás mientras esperas. Las estimaciones confirmadas también se comparten mientras este modo esté autorizado; la respuesta real no se envía hasta que terminen todos. Los movimientos no dan puntos ni confirman respuestas.

El interruptor está activado por defecto y se guarda por teléfono. También está en la sala y en el menú **⋮** del selector/espera. Ese menú permite salir sin ocupar la cabecera con otro botón. Si cualquiera lo apaga o se pierde una conexión, se retiran los valores ajenos y todos vuelven al selector/espera privados. Tu propia selección y tus confirmaciones permanecen intactas.

Todos deben usar **1.0.8 (protocolo 3)**; las versiones anteriores de LAN no son compatibles con este cambio. La espera animada sigue disponible en el modo privado.

## Hotspot de un teléfono

1. Activa **Punto de acceso / Zona Wi-Fi / Compartir Internet** en Android. El nombre cambia según el fabricante. No es necesario que tenga acceso a Internet, siempre que Android permita activar el hotspot.
2. Conecta los demás teléfonos a esa Wi-Fi. Acepta permanecer en ella si Android avisa que no tiene Internet.
3. Abre Aproximado en el teléfono que comparte la red y crea la sala. También puede alojarla otro teléfono conectado a ese hotspot.
4. Une los demás con la IP:puerto y el código de la sala. Si el descubrimiento no funciona en ese hotspot, usa la entrada manual.

El botón de ajustes abre la configuración Wi-Fi de Android. La app no enciende ni reconfigura el hotspot con APIs privadas. Actívalo **antes** de crear la sala para compartir la dirección correcta.

## Desconexiones y salida

La sala reserva la plaza y la respuesta confirmada de un invitado que pierde conexión. Reintenta automáticamente con espera progresiva. Si cierra la app o terminan sus intentos, puede volver a entrar con los mismos datos: la instalación conserva su token privado. Una confirmación pendiente se reenvía, y el anfitrión solo la acepta una vez.

Durante la partida nadie puede cambiar una respuesta confirmada. No se inventan estimaciones para un desconectado. Si no puede volver, el anfitrión puede **Volver a la sala**, confirmar que cancela el marcador y quitarlo antes de empezar otra partida. Solo se quitan participantes en la sala, nunca a mitad de un marcador.

En Android 13 o posterior se pide permiso opcional para mostrar la notificación; rechazarlo no impide jugar. Cuando las notificaciones están permitidas, la notificación permite volver a la sesión o salir. El servicio mantiene la comunicación mientras otra Activity está delante; no garantiza sobrevivir a «Forzar detención», a la muerte del proceso ni a todas las políticas de ahorro de cada fabricante. Si termina el proceso del anfitrión, la sala termina: no hay migración automática de host ni restauración de su partida LAN. La partida guardada del modo de un teléfono es independiente.

## Si no conecta

- Comprueba que todos estén en la misma red y que la IP:puerto y el código sean los actuales. El puerto y el código cambian al crear otra sala.
- Algunas Wi-Fi de invitados, hoteles o empresas aíslan clientes y bloquean la comunicación entre teléfonos. Prueba con un hotspot propio; la app no puede saltarse el aislamiento del router.
- Desactiva una VPN que intercepte la red local. El cliente elige la red Wi-Fi/Ethernet que enruta al anfitrión, aunque Android tenga datos móviles como red con Internet.
- Si cambió la IP del anfitrión, vuelve a la sala y actualiza las direcciones; vuelve a unir los invitados con los datos actuales. No se mantienen conexiones a una dirección que dejó de pertenecer al teléfono.
- Esta versión usa **IPv4 privada o link-local**. No admite redes exclusivamente IPv6, acceso remoto por Internet ni compartir la sala mediante reenvío de puertos.

## Permisos y privacidad

Android requiere el permiso normal `INTERNET` para usar sockets LAN: su presencia no significa que la app consulte Internet. Solo se aceptan IP privadas/link-local numéricas y puertos 1024–65535, sin resolver nombres ni usar servicios externos. `ACCESS_NETWORK_STATE` selecciona la red local; `ACCESS_WIFI_STATE` y `CHANGE_WIFI_MULTICAST_STATE` permiten el descubrimiento local opcional. No se solicita ubicación, contactos, cámara ni escaneo de redes Wi-Fi.

Los permisos foreground connectedDevice permiten una notificación de sesión activa. Se mantiene un wake lock de CPU limitado a dos horas y la pantalla encendida mientras se juega en LAN. Al salir se liberan sockets, anuncio NSD, búsqueda y locks; no hay búsqueda permanente ni servicio de sesión cuando no se está jugando.

El código autoriza a participantes de confianza. El tráfico usa P-256 ECDH efímero, autenticación HMAC de la transcripción con PBKDF2-SHA256 (120.000 iteraciones), HKDF-SHA256 y AES-256-GCM con contadores/direcciones separados. Conocer el código no permite descifrar pasivamente las otras conexiones. No es una garantía frente a un participante que conozca el código y controle activamente la red; no se ha contratado una auditoría criptográfica externa. No compartas el código fuera de la sala.

El anfitrión es la autoridad del juego y posee su base de preguntas. Hasta la revelación, el protocolo no envía la respuesta, la fuente, los IDs del catálogo, las preguntas futuras ni las estimaciones ajenas cuando el modo es privado. Con consentimiento unánime, «Regla en vivo» comparte borradores y valores confirmados para mantener el tablero completo; el objetivo real sigue secreto. No es un sistema antitrampas contra un anfitrión o un teléfono modificado. El token de reconexión y los datos de entrada se guardan solo en preferencias privadas, sin backup.

## Diseño y comprobaciones

TCP nativo, sin bibliotecas de red externas. Frames de hasta 64 KiB, cadenas y listas acotadas, máximo doce sockets admitidos (incluidas autenticaciones), máximo ocho plazas y límite de doce comandos por segundo por invitado. Los estados completos se actualizan solo ante acciones discretas y se conserva únicamente el último estado encolado para un receptor lento. Latidos cada cuatro segundos y timeout de lectura de quince segundos. Con regla en vivo habilitada, los borradores se coalescen a 8 Hz (último valor por emisor, frames pequeños con hasta siete valores por receptor); no generan snapshots completos. Límite separado de 32 borradores entrantes/s por invitado. Deshabilitada la función, no se envían movimientos. Partida/ronda/revisión y precisión se validan en origen, anfitrión y destino para descartar mensajes atrasados. Los borradores no se guardan y se eliminan al confirmar, desconectar o cambiar la política/ronda. Una barra confirmada se reconstruye con el valor inmutable ya aceptado; al deshabilitar el modo también se retira de los frames compartidos.

Las pruebas automatizadas ejecutan partidas 2/8 en el motor, una partida de cinco rondas con ocho participantes por sockets reales, reconexión conservando respuesta, corte de red con reenvío, código incorrecto, privacidad de snapshots, límites, valores inválidos, mensajes tardíos, duplicados, manipulación y replay. Las pruebas de 1.0.8 añaden grupos 2/3/4/8, consentimiento, borradores inválidos/atrasados, coalescencia y sockets con tres y ocho participantes sin alterar puntuaciones. La interacción del selector se verifica mediante acciones de accesibilidad y un gesto táctil real sobre una columna ajena, incluyendo retirada y restauración de las ocho barras al cambiar el consentimiento. Robolectric comprueba recepción y corte de borradores, preferencias persistentes, el servicio foreground, recreación de ViewModel y puntuación equivalente en ambos teléfonos proyectados. Los renders cubren las pantallas LAN en 360×800 (2/8), 320×640 y 411×914 con fontScale 1.3.

**Límite de verificación:** sockets reales en loopback y Android simulado; este entorno no dispone de dos teléfonos físicos ni de emulador con hotspot. Antes de dar por verificado un fabricante/red concretos, comprobar en dispositivos: Wi-Fi sin Internet, hotspot alojado y unido, datos móviles activos, búsqueda NSD/manual, ocho participantes si están disponibles, bloqueo/cambio de app, corte y vuelta de Wi-Fi y cierre del anfitrión. No se atribuyen esas pruebas físicas a las comprobaciones automatizadas.
