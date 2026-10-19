package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.camera.core.UseCase
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.ErrorDominio
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BarraSuperior
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.feature.incidencias.domain.POR_SINCRONIZAR
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * HU-09 (RF-924, prototipo 14): «Evidencia fotográfica». Pide el permiso al entrar (RF-916) y toma las fotos con
 * CameraX. «Listo» y «Volver» hacen lo mismo: cada foto ya se guardó al tomarla (RF-930).
 */
@Composable
fun EvidenciaFotograficaRoute(
    alVolver: () -> Unit,
    vm: CapturaEvidenciaViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val captura = remember { crearCapturaDeFoto() }
    val casos = remember { listOf<UseCase>(captura) }
    var camaraLista by remember { mutableStateOf(false) }
    // La foto se toma fuera del LaunchedEffect: al consumir el evento cambia su clave y lo cancelaría a medias
    val alcance = rememberCoroutineScope()
    LaunchedEffect(estado.fotoPorTomar) {
        val foto = estado.fotoPorTomar ?: return@LaunchedEffect
        vm.alFotoEnCamara()
        alcance.launch {
            // RF-925 y RF-928: si la cámara falla, el ViewModel descarta lo que haya escrito
            if (captura.tomarFoto(foto.ruta)) vm.alFotoTomada(foto) else vm.alFalloCaptura(foto)
        }
    }
    EvidenciaFotograficaScreen(
        estado = estado,
        alVolver = alVolver,
        alCambiarTipo = vm::alCambiarTipo,
        alDisparar = vm::alDisparar,
        vistaCamara = {
            VistaCamara(casos, Modifier.fillMaxSize())
            // Sin permiso no se dibuja la cámara y el disparador queda deshabilitado
            DisposableEffect(Unit) {
                camaraLista = true
                onDispose { camaraLista = false }
            }
        },
        camaraLista = camaraLista,
        conPermisoCamara = { contenido ->
            ConPermisoCamara(textoNegado = null, modifier = Modifier.fillMaxSize(), contenido = contenido)
        }
    )
}

@Composable
fun EvidenciaFotograficaScreen(
    estado: CapturaEvidenciaUiState,
    alVolver: () -> Unit,
    alCambiarTipo: (TipoEvidencia) -> Unit,
    alDisparar: () -> Unit,
    vistaCamara: @Composable () -> Unit,
    camaraLista: Boolean = true,
    // Sin permiso se ve la explicación en lugar de la cámara (RF-916); las vistas previas no lo piden
    conPermisoCamara: @Composable (contenido: @Composable () -> Unit) -> Unit = { contenido -> contenido() }
) {
    // D-10: la barra clara del rediseño (#39) en vez de la barra oscura con «X» del prototipo
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { BarraSuperior("Evidencia fotográfica", alVolver = alVolver) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.sm)
        ) {
            // BarraSuperior no tiene subtítulo: el código de la incidencia y el del equipo van debajo
            estado.incidencia?.let { incidencia ->
                Text(
                    "${incidencia.codigoVisible ?: POR_SINCRONIZAR} · ${incidencia.codigoEquipo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SelectorTipo(estado.tipo, alCambiarTipo)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
            ) {
                conPermisoCamara {
                    Box(Modifier.fillMaxSize()) {
                        vistaCamara()
                        Indicacion(
                            "Se guardará como: ${nombreDelTipo(estado.tipo)}",
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = Espaciado.m)
                        )
                    }
                }
            }
            // RF-929, RF-931: el bloqueo es un aviso; RF-928: el error de la última foto
            estado.bloqueo?.let { Aviso(mensajeFotos(it), MaterialTheme.colorScheme.onSurfaceVariant) }
                ?: estado.error?.let { Aviso(mensajeFotos(it), MaterialTheme.colorScheme.error) }
            if (estado.fotos.isNotEmpty()) FilaMiniaturas(estado.fotos)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = Espaciado.m)
            ) {
                Text(
                    cantidadDeFotos(estado.fotos.size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Disparador(estado.puedeDisparar && camaraLista, alDisparar, Modifier.align(Alignment.Center))
                BotonPrincipal(onClick = alVolver, modifier = Modifier.align(Alignment.CenterEnd)) { Text("Listo") }
            }
        }
    }
}

@Composable
private fun Aviso(texto: String, color: Color) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.fillMaxWidth())
}

/** RF-925: las fotos de la incidencia, de la más antigua a la más reciente, con su rótulo. */
@Composable
private fun FilaMiniaturas(fotos: List<Evidencia>) {
    val rotulos = rotulosFotos(fotos)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
        itemsIndexed(fotos, key = { _, foto -> foto.id }) { indice, foto ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MiniaturaEvidencia(foto.rutaArchivo, LadoMiniatura, descripcion = rotulos[indice])
                Text(
                    rotulos[indice],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** P-11: el disparador es circular, la forma que todos reconocen; deshabilitado cambia de color (RF-927). */
@Composable
private fun Disparador(habilitado: Boolean, alDisparar: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (habilitado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = alDisparar,
        enabled = habilitado,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(GrosorDisparador, color),
        modifier = modifier
            .size(TamanoDisparador)
            .semantics { contentDescription = "Tomar foto" }
    ) {
        Box(
            Modifier
                .padding(Espaciado.s)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** «Falla 1», «Falla 2», «Pieza 1»…: cada tipo se numera por separado, en el orden en que se tomaron. */
internal fun rotulosFotos(fotos: List<Evidencia>): List<String> {
    val vistas = mutableMapOf<TipoEvidencia, Int>()
    return fotos.map { foto ->
        val numero = vistas.merge(foto.tipo, 1, Int::plus)
        when (foto.tipo) {
            TipoEvidencia.FALLA -> "Falla $numero"
            TipoEvidencia.PIEZA_CAMBIADA -> "Pieza $numero"
        }
    }
}

internal fun cantidadDeFotos(cantidad: Int): String = if (cantidad == 1) "1 foto" else "$cantidad fotos"

private val LadoMiniatura = 64.dp
private val TamanoDisparador = 72.dp
private val GrosorDisparador = 4.dp

/** RF-924: «Falla» o «Pieza cambiada», con las esquinas del tema en vez de la píldora de Material 3. */
@Composable
private fun SelectorTipo(tipo: TipoEvidencia, alCambiarTipo: (TipoEvidencia) -> Unit) {
    val opciones = listOf(TipoEvidencia.FALLA to "Falla", TipoEvidencia.PIEZA_CAMBIADA to "Pieza cambiada")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { indice, (opcion, texto) ->
            SegmentedButton(
                selected = tipo == opcion,
                onClick = { alCambiarTipo(opcion) },
                shape = SegmentedButtonDefaults.itemShape(indice, opciones.size, MaterialTheme.shapes.small)
            ) { Text(texto) }
        }
    }
}

internal fun nombreDelTipo(tipo: TipoEvidencia): String = when (tipo) {
    TipoEvidencia.FALLA -> "foto de la falla"
    TipoEvidencia.PIEZA_CAMBIADA -> "foto de la pieza cambiada"
}

// ---------- Vistas previas: la pantalla se prueba sin Room, ViewModel ni cámara ----------

private val incidenciaDePrueba = Incidencia(
    id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende",
    usuarioResponsable = "María Quispe", fechaRegistro = Instant.parse("2026-10-11T10:42:00Z"),
    registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas",
    marca = MarcaSeguimiento.EN_ATENCION, tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
)

private val listaParaTomar = CapturaEvidenciaUiState(cargando = false, incidencia = incidenciaDePrueba)

private fun fotosDePrueba(cantidad: Int) = List(cantidad) { i ->
    Evidencia(
        "f$i", incidenciaDePrueba.id, if (i % 3 == 2) TipoEvidencia.PIEZA_CAMBIADA else TipoEvidencia.FALLA,
        "/no-existe/f$i.jpg", Instant.parse("2026-10-18T15:00:00Z").plusSeconds(i * 60L)
    )
}

@Preview(name = "Evidencia · claro", showBackground = true, heightDp = 760)
@Composable
private fun EvidenciaClaro() = SigtecTheme { Surface { Previa(listaParaTomar) } }

@Preview(name = "Evidencia · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 760)
@Composable
private fun EvidenciaOscuro() = SigtecTheme(darkTheme = true) {
    Surface { Previa(listaParaTomar.copy(tipo = TipoEvidencia.PIEZA_CAMBIADA)) }
}

@Preview(name = "Con fotos, guardando · claro", showBackground = true, heightDp = 760)
@Composable
private fun GuardandoClaro() = SigtecTheme {
    Surface { Previa(listaParaTomar.copy(fotos = fotosDePrueba(3), guardando = true)) }
}

@Preview(name = "Al máximo · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 760)
@Composable
private fun AlMaximoOscuro() = SigtecTheme(darkTheme = true) {
    Surface { Previa(listaParaTomar.copy(fotos = fotosDePrueba(10), bloqueo = ErrorDominio.LimiteFotosAlcanzado)) }
}

@Composable
private fun Previa(estado: CapturaEvidenciaUiState) = EvidenciaFotograficaScreen(
    estado = estado,
    alVolver = {},
    alCambiarTipo = {},
    alDisparar = {},
    vistaCamara = { VistaCamaraDeMuestra() }
)
