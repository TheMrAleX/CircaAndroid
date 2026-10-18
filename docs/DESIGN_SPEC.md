FASE 3 – DISEÑO VISUAL (SPEC CERRADA, OBLIGATORIA)

REGLAS DE DISEÑO
- Guarda TODO este bloque tal cual en /docs/DESIGN_SPEC.md y consúltalo antes de escribir cada pantalla.
- Si existen /docs/reference/ref-selector.png y ref-reveal.png, ábrelas: son la referencia visual del Selector y la Revelación. Replica su estética (fondo oscuro, reglas con marcas a los lados, número gigante con contorno, barras de color por jugador) con arte propio.
- PROHIBIDO usar componentes Material 3 con su estilo por defecto (TopAppBar, Card con elevación, Button, FilterChip, Switch sin tema). Usa Material solo como base y construye componentes propios con el sistema de abajo.
- Primero construye el sistema de diseño (Theme.kt, Tokens.kt, Components.kt) y un @Preview de cada componente. Luego las pantallas, en el orden de esta spec. Cada pantalla debe tener @Preview con 2 y con 8 jugadores.
- Dispositivo de referencia: 360x800dp. Verifica también 320x640dp, 411x914dp y fontScale 1.3 sin recortes ni solapes. Si puedes, usa Paparazzi o Roborazzi para renderizar PNGs a /docs/screens y revísalos; corrige lo que se vea mal.
- La app es SOLO TEMA OSCURO en v1. Quita "tema" de Ajustes.
- Toda animación a 60 fps: usa animate*AsState, Animatable y Canvas; nada de recomposiciones pesadas dentro de un drag.

TOKENS
Colores base:
  bg            #262A35  (fondo de pantallas)
  surface       #313644  (tarjetas, filas)
  surfaceHigh   #3B4152
  onBg          #FFFFFF
  onBgMuted     #FFFFFF con 65% alpha
  tick          #FFFFFF con 55% alpha (marcas menores), 85% (mayores)
  danger        #FF5A4D
  gold          #FFC83D (corona)
Colores de jugador (color / tinta oscura para texto encima / tinta clara para contornos):
  1 Azul      #2F7CF6 / #0B2A66 / #8DBBFF
  2 Naranja   #F5A623 / #5A3600 / #FFD08A
  3 Rosa      #FF5C8A / #5E0F2A / #FFA3BD
  4 Verde     #2ED47A / #0B4A28 / #92F0BC
  5 Morado    #9B6BFF / #2E1470 / #CDB5FF
  6 Rojo      #FF5A4D / #5E140D / #FFA59E
  7 Turquesa  #22C7D6 / #07454C / #8FEAF2
  8 Amarillo  #FFD93D / #4D3F00 / #FFEC99
  Cada jugador tiene un data class PlayerStyle(color, ink, inkLight). Por defecto el jugador N usa el color N; se puede cambiar en configuración sin repetir.
Tipografía:
  Fuente display/títulos: intenta descargar e incluir en res/font una fuente redondeada OFL (Fredoka o Baloo 2) con pesos SemiBold y Bold (Black si existe). Si no puedes descargarla, usa la fuente del sistema con FontWeight.Black (900) y FontWeight.ExtraBold. Cuerpo: Nunito o la fuente del sistema.
  displayXL 96sp Black (número del selector y de la revelación; con autoajuste de tamaño hasta mínimo 36sp para que quepa en ancho de pantalla - 48dp)
  displayL 44sp Black (nombre de la app, nombre en pase de turno 40sp)
  title 22sp ExtraBold, subtitle 18sp SemiBold, body 16sp Medium, caption 13sp Medium
Espaciado en múltiplos de 4dp. Padding horizontal de pantalla: 20dp. Separación entre secciones: 16dp.
Radios: 12 (chips pequeños), 20 (filas), 28 (tarjetas y sheets), 40 (sheet inferior del selector), pill = 50%.
Movimiento: 150ms (micro), 250ms (cambios de estado), 400ms (entradas), 700-900ms (revelación). Easing FastOutSlowIn; springs con dampingRatio 0.7. Transiciones entre pantallas: fundido + deslizamiento vertical de 24dp en 300ms.
Formato de números: separador de miles con punto (55.000); decimales con coma. Si no cabe: sufijos compactos "17 mil", "1,4 M" (millones), "1.400 M", y "B" para 10^12. Usa una única función formatNumber(value, compact: Boolean) testeada.

COMPONENTES BASE (construir y reutilizar)
1) GameButton (primario): alto 60dp, ancho completo menos 40dp, forma pill, relleno sólido del color indicado, texto 20sp Black en color tinta. Efecto "chunky": borde inferior sólido 4dp de un tono 25% más oscuro; al presionar se desplaza 4dp hacia abajo y el borde desaparece (80ms) + háptica ligera. Variantes: primario (color de acento Azul #2F7CF6 con texto blanco), claro (fondo blanco, texto tinta), secundario (contorno 2dp blanco 40% alpha, texto blanco, sin relleno).
2) TopBar propia: alto 56dp, título centrado 16sp SemiBold blanco, botón cerrar (X) o atrás a 24dp dentro de un área táctil de 48dp. Sin sombra ni fondo.
3) RulerCanvas: dibuja dos reglas, una pegada al borde izquierdo y otra al borde derecho de la pantalla, con marcas horizontales que se extienden hacia el centro desde cada borde. Una marca cada 12dp. Largo: menor 20dp, media (cada 5ª) 36dp, mayor (cada 10ª) 56dp. Grosor 2dp, extremos redondeados. Opacidad: tick 55% / 85% en las mayores. Fundido de 24dp en los bordes superior e inferior del área. Se mueve con el scroll.
4) BigNumber: texto displayXL en dos pasadas con Canvas/drawText: primero trazo (Stroke, 6dp, color inkLight del jugador, esquinas redondeadas) y encima relleno (color ink del jugador). Centrado, una sola línea, autoajuste de tamaño.
5) PlayerAvatar: círculo del color del jugador con la inicial en 20sp Black color tinta (tamaños 32/48/120dp).
6) Pill de valor: fondo #1F222B al 90% alpha, radio 16dp, alto 40dp, padding horizontal 12dp, texto blanco 18sp Black.
7) Confetti: capa Canvas con 60 partículas (círculos y óvalos pequeños de 4-10dp en los colores de jugador y blanco), explosión desde un punto, gravedad, rotación y fundido, 1.8s.

PANTALLAS

0) APP ICON
Ícono adaptativo: fondo #262A35; primer plano: tres barras verticales redondeadas (azul, naranja, rosa) de alturas distintas con una línea horizontal blanca cruzándolas. Sin texto.

1) INICIO
- Fondo bg. RulerCanvas en ambos bordes que se desplaza lentamente hacia arriba (loop de 20s, linear). Un número gigante "42.000" a 8% alpha detrás del contenido como decoración.
- Contenido centrado verticalmente, columna: nombre de la app ("Aproximado") en displayXL 44sp con el mismo efecto de contorno del BigNumber en azul; debajo caption "Adivina el número. Acércate más que los demás." (onBgMuted), separación 8dp.
- Separación 48dp, luego botones apilados con 12dp entre ellos: "Jugar" (primario azul), "Continuar partida" (claro, SOLO si hay partida guardada, mostrando "Ronda X/Y · N jugadores" en caption dentro del botón), "Ajustes" (secundario).
- Entrada: el título cae con spring (desde -40dp, 400ms) y los botones aparecen en cascada con 80ms de retraso.

2) CONFIGURAR PARTIDA
- TopBar con flecha atrás y título "Nueva partida". Contenido en LazyColumn con padding 20dp y botón inferior fijo "Empezar" (primario azul, con 16dp sobre la barra de navegación; degradado de bg a transparente de 24dp arriba del botón para que el contenido se desvanezca).
- Cada sección es una tarjeta: fondo surface, radio 28dp, padding 16dp, título de sección 18sp ExtraBold, 12dp de separación entre título y contenido.
- Tarjeta "Jugadores": fila central con botón "−" (círculo 56dp, surfaceHigh, icono blanco 28dp), el número de jugadores en displayL 44sp Black blanco, botón "+" igual (el + en azul). Debajo caption "De 2 a 8". Botones se desactivan al 40% alpha en los límites. Debajo, la lista de jugadores (animateContentSize; al subir el número aparece una fila con fade+expand 250ms): cada fila de 64dp con PlayerAvatar 40dp (tocarlo abre un sheet con los 8 colores en cuadrícula de 4x2, los usados por otros quedan atenuados), campo de nombre (texto 18sp SemiBold blanco, sin caja, línea inferior 2dp del color del jugador, placeholder "Jugador N"), máximo 12 caracteres.
- Tarjeta "Rondas": 4 opciones 5/10/15/20 en fila de segmentos iguales (alto 48dp, radio pill). Seleccionado: relleno azul, texto blanco Black. No seleccionado: surfaceHigh, texto onBgMuted.
- Tarjeta "Categorías": FlowRow de chips (alto 40dp, radio pill, emoji + nombre 14sp SemiBold). Primer chip "Todas". Seleccionado: relleno azul; no seleccionado: contorno 2dp blanco 25%. Mínimo 1 seleccionada. Debajo muestra caption "N preguntas disponibles" y, si no alcanza para las rondas elegidas, mensaje en color danger y botón Empezar desactivado.
- Tarjeta "Dificultad": 4 segmentos Fácil / Media / Difícil / Mixta, mismo estilo que Rondas.

3) INTRO DE RONDA
- Fondo bg, sin reglas. Centro: caption "RONDA 3 DE 10" (letter-spacing 2sp, onBgMuted), debajo chip de categoría con emoji (surface, radio pill), debajo la pregunta en 28sp ExtraBold, blanca, centrada, máximo 6 líneas, padding horizontal 28dp. Abajo botón claro "Empezar ronda". Aparece con fundido+escala 0.96→1 en 400ms.
- Texto auxiliar "Todos lean la pregunta y luego pásense el celular" en caption.

4) PASE DE TURNO
- Fondo a pantalla completa del color del jugador que sigue (transición con revelado circular desde el centro, 350ms). Arriba, fila de puntos de progreso (un punto por jugador, 10dp, el actual 16dp blanco, los que ya jugaron al 100% alpha tinta, los pendientes al 30%).
- Centro: caption "Pásale el celular a" (18sp, tinta 80%), PlayerAvatar 120dp con la inicial a 56sp (fondo: blanco 25% alpha, texto tinta), debajo nombre en 40sp Black tinta, máximo 2 líneas.
- Abajo: botón claro "Soy yo, listo" (fondo blanco, texto tinta), y caption "Que nadie más mire la pantalla" encima del botón.
- Al tocar el botón, transición a la pantalla del selector donde ese mismo color sube desde abajo como el sheet.

5) SELECTOR DE NÚMERO (replica ref-selector.png)
Layout vertical (referencia 360x800):
  ┌──────────────────────────┐
  │ Nico · 3/10            ✕ │  TopBar 56dp
  │▌━━                    ━━▐│  reglas laterales
  │▌─  (más arriba = más)  ─▐│
  │▌━━                    ━━▐│
  │        55.000            │  BigNumber, centro vertical en el borde superior del sheet (a 58% de la altura)
  │ ████████████████████████ │  sheet del color del jugador
  │  ¿Cuántas personas…?     │  pregunta
  │   (−)    [123]    (+)    │
  │ [       Confirmar      ] │
  └──────────────────────────┘
- Fondo bg. Reglas laterales ocupan desde bajo la TopBar hasta el borde superior del sheet (se ven detrás/encima del número, nunca detrás del sheet).
- Sheet: ocupa el 42% inferior de la pantalla (mínimo 300dp), color del jugador, esquinas superiores 40dp, llega hasta el borde inferior de pantalla (edge-to-edge), padding horizontal 24dp, padding superior 56dp (para dejar espacio a la mitad inferior del número que lo solapa).
- BigNumber centrado horizontalmente, su centro vertical alineado exactamente con el borde superior del sheet. Si la pregunta tiene unidad, muéstrala como caption 16sp tinta 70% justo debajo del número, dentro del sheet.
- Pregunta: 20sp SemiBold, color tinta, centrada, máx 5 líneas.
- Fila de ajuste fino: botón "−" y "+" círculos de 48dp (fondo tinta al 15% alpha, icono tinta) a los lados; al centro botón de teclado (icono, círculo 48dp igual) que abre un ModalBottomSheet con campo numérico grande (displayL), teclado numérico, validación y botón "Aceptar".
- Botón "Confirmar": pill 60dp, fondo blanco, texto tinta 20sp Black, margen inferior 20dp + inset de la barra de navegación.
- Interacción: drag vertical sobre toda el área de reglas, con fling (decay) y límites con rebote suave. El contenido de la regla sigue al dedo (arrastrar hacia abajo = número más alto). El valor es el que queda bajo la línea central del número. Mientras arrastras, el valor se redondea al paso "bonito" de la marca más cercana (pasos 1, 2, 5 × 10^k); no uses valores con decimales raros.
- Escala: el rango [min_plausible, max_plausible] se divide en 1000 marcas menores. Lineal: marcas equiespaciadas. Log: marcas equiespaciadas en logaritmo. Posición inicial: el punto medio de la escala (media geométrica en log); si coincide con la respuesta, desplaza el inicio 7% para no filtrar nada.
- Háptica: tick ligero al cruzar cada marca mayor (con límite de 1 cada 40ms), confirmación más fuerte al presionar Confirmar. Sonido opcional tipo "clic" suave en marcas mayores.
- Al confirmar: el sheet se desliza hacia abajo 250ms y pasa al siguiente jugador (pase de turno) o a la revelación.
- La X abre un diálogo propio (surface, radio 28dp): "¿Salir de la partida?" con "Seguir jugando" (primario) y "Salir" (secundario). La partida queda guardada.

6) REVELACIÓN (replica ref-reveal.png)
Layout:
  ┌──────────────────────────┐
  │ Respuesta · Ronda 3/10 ✕ │
  │ pregunta (13sp, 2 líneas)│
  │▌            ┌👑──┐      ▐│
  │▌            │65.000│    ▐│  barra azul (jugador más cercano)
  │▌─ ─ ─ 55.000 ─ ─ ─ ─ ─ ─ │  línea de respuesta + número gigante
  │▌            │      │    ▐│
  │▌ ┌17.000┐   │      │    ▐│
  │▌ │ nico │   │      │    ▐│  barra naranja
  │▌ └──────┘   └──────┘    ▐│
  │       [ Continuar ]       │
  └──────────────────────────┘
- Fondo bg con RulerCanvas igual que el selector. La escala del área se ajusta a [min, max] de (respuesta + todas las estimaciones) con 15% de margen arriba y abajo (misma escala lineal/log de la pregunta). Mayor valor = más arriba. Etiquetas de valor en las marcas mayores de la regla izquierda, 11sp, blanco 50%, formato compacto.
- Cada jugador es una barra vertical: esquinas superiores 20dp, color del jugador, cuyo borde superior está en la Y de su estimación y que baja hasta el borde inferior de la pantalla. Se colocan en columnas de izquierda a derecha ordenadas por valor ascendente, con ancho = (anchoPantalla - 24dp) / N + 12dp de solapamiento. Las barras más bajas se dibujan delante de las más altas.
- Dentro de cada barra, arriba (8dp bajo su borde superior): Pill de valor. Debajo, el nombre en 12sp ExtraBold tinta (si el ancho de columna es < 72dp, muestra solo PlayerAvatar de 32dp con inicial y valor en formato compacto con 14sp). Máximo 8 barras: asegúrate de que con 8 jugadores nada se solape ilegiblemente (si dos pills coinciden en Y, desplaza la de la barra más baja 44dp hacia abajo).
- Respuesta real: línea horizontal blanca de 2dp (con guiones 12/8dp) en su Y, a todo el ancho, con etiqueta "Respuesta" (caption en pill blanca con texto tinta oscura) en el borde izquierdo. El número gigante (displayXL 88sp Black) centrado sobre la línea: relleno blanco 25% alpha, contorno blanco 70% 4dp (efecto cristal), dibujado ENCIMA de las barras.
- Ganador de la ronda: corona dorada (icono vectorial propio de 28dp, color gold) sobre su Pill, con borde dorado de 3dp en la barra.
- Secuencia (cualquier toque salta al estado final):
  t=0.0s  Aparecen reglas y barras a la altura 0 (en la base).
  t=0.2–1.1s  Las barras suben a su Y, escalonadas 120ms en orden de turno, con ease-out y 5% de overshoot; los valores de las pills cuentan de 0 al valor final (900ms).
  t=1.4s  La línea de respuesta barre de izquierda a derecha (500ms) y el número gigante entra con escala 0.8→1 y fundido (400ms) + háptica fuerte.
  t=2.2s  Desde cada barra baja o sube una línea punteada del color del jugador hasta la línea de respuesta con un chip de diferencia ("+10.000" / "−38.000", 12sp Black, fondo del color del jugador, texto tinta).
  t=2.8s  Cae la corona con spring sobre el ganador + Confetti desde su pill. Los chips de puntos "+3", "+2", "+1" (pill del color del jugador, 16sp Black) vuelan hacia arriba desde cada pill y se desvanecen.
  t=3.4s  Aparece el botón claro "Continuar" abajo (fundido 250ms).
- Si hay acierto exacto o a menos de 1%: etiqueta "¡Clavado!" (pill dorada) junto a la corona.

7) MARCADOR ENTRE RONDAS
- TopBar "Marcador · Ronda 3/10" (sin X). Lista de jugadores ordenada por puntos totales (de mayor a menor), reordenamiento animado (animateItem, spring) cuando cambian posiciones.
- Cada fila: alto 72dp (64dp si hay más de 6 jugadores), fondo surface, radio 20dp, barra izquierda de 6dp del color del jugador, padding 16dp. Contenido: número de posición 24sp Black (el 1º en gold), PlayerAvatar 48dp, nombre 18sp SemiBold, y a la derecha puntos totales 28sp Black. Chip "+N" (color del jugador, texto tinta) que aparece a la izquierda del total y se "absorbe" sumándose al total en 600ms con conteo animado.
- Botón inferior fijo claro: "Siguiente ronda" o, tras la última, "Ver resultado final".

8) RESULTADO FINAL
- Fondo bg + Confetti continuo suave en el fondo (ráfaga inicial grande).
- Podio centrado, columnas pegadas con 8dp de separación: 2º (izquierda, altura 120dp), 1º (centro, 160dp), 3º (derecha, 90dp). Cada columna: color del jugador, esquinas superiores 20dp, el número de posición dentro en 48sp Black tinta, avatar 56dp sobre la columna y nombre 16sp ExtraBold debajo del avatar, puntos totales 18sp Black. Corona gold sobre el 1º. Empates: comparten posición y columna con mismo alto.
- Animación: sube 3º (500ms), luego 2º, luego 1º con spring y háptica; al final cae la corona + ráfaga de confetti.
- Debajo del podio: lista compacta (alto 56dp) de los jugadores restantes (puestos 4 a 8).
- Botones fijos abajo: "Jugar otra vez" (primario azul, misma configuración) y "Menú" (secundario).

9) AJUSTES
- TopBar atrás. Filas de 64dp sobre tarjeta surface radio 28dp: "Sonido" (switch), "Vibración" (switch), "Borrar historial y partida guardada" (texto danger, con diálogo de confirmación), "Acerca de" (versión, licencia MIT y el aviso: "No está afiliada a ningún juego existente"). Switch propio: pista 52x32dp, activo azul, inactivo surfaceHigh, círculo blanco 24dp.

CONTROL DE CALIDAD VISUAL (hazlo antes de cerrar la fase)
- Ningún texto cortado con fontScale 1.3 ni en 320x640dp.
- Con 8 jugadores la Revelación y el Marcador son legibles y sin solapes.
- Todo elemento tocable mide ≥48dp y tiene contentDescription.
- El número gigante nunca se sale de pantalla (probado con 1.400.000.000 y con 0,5).
- Ninguna pantalla muestra la estimación de un jugador a otro antes de la Revelación (el pase de turno limpia la pantalla anterior; la pantalla del selector no aparece en la vista de apps recientes: aplica FLAG_SECURE solo en esa pantalla si no afecta al resto).
- Anota en DECISIONS.md cualquier desvío de esta spec y por qué.

## Corrección del usuario para 1.0.2 (prioritaria sobre las revisiones previas)

La revelación debe conservar la gráfica vertical de barras. La regla del selector debe quedar fija; el panel del color del jugador y el número se mueven arriba y abajo en tiempo real. Eliminar el aviso de no mirar la pantalla ajena. Estabilizar los decimales y la tipografía al cambiar el número, incluyendo 3,32 → 3,40. Conservar controles accesibles y no solapar etiquetas con 2–8 jugadores.

## Corrección del usuario para 1.0.3

Los conteos usan enteros; solo unas pocas medidas admiten decimales mediante metadatos explícitos. La gráfica no debe exagerar la separación de 9,99 y 8,99: las barras lineales parten de cero con alturas proporcionales y eje numérico visible. Las escalas logarítmicas conservan el dominio de la pregunta.

## Corrección del usuario para 1.0.4

Omitir el pase de turno. Tras confirmar, mostrar directamente el selector del siguiente jugador con avatar, nombre, turno y ronda en una cabecera integrada. Reiniciar la estimación y conservar FLAG_SECURE. Conservar las cifras publicadas sin redondear objetivos para facilitar aciertos; en la revelación, permitir leer la cifra completa junto a la representación gráfica.

## Extensión LAN 1.0.5

Petición del usuario: modo LAN nativo para 2–8 teléfonos en la misma Wi-Fi o en el hotspot de uno de ellos. Reutilizar tema, regla móvil, gráfica y podio. Añadir entrada LAN, creación/unión, sala con configuración del anfitrión, estados de conexión y espera de respuestas. Respuestas simultáneas y privadas hasta confirmar todos. Nombre y color propios siempre visibles. Controles del anfitrión diferenciados de la espera de los invitados. Mantener áreas táctiles de 48 dp y contenido desplazable a 320×640/fontScale 1.3.

## Espera LAN 1.0.6

Tras confirmar antes que los demás, mostrar «Esperando resultados…» con un anillo propio giratorio alrededor del avatar, en el color del jugador. Giro con EaseInOutCubic (.65, 0, .35, 1) de 2,2 s y respiración sutil de 1,1 s. Animar solo la capa gráfica, sin recomponer la sala ni emitir tráfico. Mantener contador real de confirmaciones, lista desplazable y controles existentes; la revelación sigue dependiendo del anfitrión y de que confirmen todos. Respetar la escala de animaciones de Compose; previews/renders usan una fase fija para comparar el layout.
