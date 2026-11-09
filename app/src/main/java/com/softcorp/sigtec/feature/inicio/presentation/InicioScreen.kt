package com.softcorp.sigtec.feature.inicio.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.Incidencia
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.AreaTactilMinima
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.Avatar
import com.softcorp.sigtec.core.ui.components.BotonTexto
import com.softcorp.sigtec.core.ui.components.CabeceraHero
import com.softcorp.sigtec.core.ui.components.EncabezadoSeccion
import com.softcorp.sigtec.core.ui.components.EtiquetaIncidencia
import com.softcorp.sigtec.core.ui.components.Tarjeta
import com.softcorp.sigtec.core.ui.components.TarjetaMetrica
import com.softcorp.sigtec.core.ui.components.VarianteMetrica
import com.softcorp.sigtec.feature.atencion.domain.EstadoSolicitudVisible
import com.softcorp.sigtec.feature.atencion.presentation.textoEstadoSolicitud
import com.softcorp.sigtec.feature.incidencias.domain.codigoParaMostrar
import com.softcorp.sigtec.feature.incidencias.presentation.diasPendiente
import com.softcorp.sigtec.feature.incidencias.presentation.responsableConArea
import com.softcorp.sigtec.feature.inicio.domain.Metrica
import com.softcorp.sigtec.feature.inicio.domain.RepuestoProximo
import com.softcorp.sigtec.feature.inicio.domain.ResumenInicio
import com.softcorp.sigtec.feature.inicio.domain.TipoMetrica
import com.softcorp.sigtec.feature.inicio.domain.VistaInicio
import java.time.Instant

private val TamanoAvatar = 56.dp
private val TamanoAcceso = 64.dp

@Composable
fun InicioRoute(
    alAbrirAjustes: () -> Unit,
    alAbrirMisTareas: () -> Unit,
    alAbrirLista: () -> Unit,
    alAbrirDetalle: (String) -> Unit,
    alRegistrar: () -> Unit,
    alEscanear: () -> Unit,
    alAbrirRepuestos: () -> Unit,
    vm: InicioViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    InicioScreen(
        estado = estado,
        ahora = Instant.now(),
        alAbrirAjustes = alAbrirAjustes,
        // RF-I4 a RF-I6: el técnico va a su bandeja; los demás, a la lista
        alTocarMetrica = { if (estado.resumen?.vista == VistaInicio.TECNICO) alAbrirMisTareas() else alAbrirLista() },
        alAbrirDetalle = alAbrirDetalle,
        alRegistrar = alRegistrar,
        alEscanear = alEscanear,
        alAbrirRepuestos = alAbrirRepuestos
    )
}

@Composable
fun InicioScreen(
    estado: InicioUiState,
    ahora: Instant,
    alAbrirAjustes: () -> Unit,
    alTocarMetrica: () -> Unit,
    alAbrirDetalle: (String) -> Unit,
    alRegistrar: () -> Unit,
    alEscanear: () -> Unit,
    alAbrirRepuestos: () -> Unit
) {
    val resumen = estado.resumen
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Cabecera(estado, alAbrirAjustes, alTocarMetrica)
        if (resumen != null) {
            Column(
                Modifier.padding(Espaciado.m),
                verticalArrangement = Arrangement.spacedBy(Espaciado.l)
            ) {
                resumen.atendiendo?.let { AtendiendoAhora(it, ahora) { alAbrirDetalle(it.id) } }
                if (resumen.puedeRegistrar) AccesosRapidos(alRegistrar, alEscanear)
                resumen.repuesto?.let { TarjetaRepuesto(it, alAbrirRepuestos) }
            }
        }
    }
}

@Composable
private fun Cabecera(estado: InicioUiState, alAbrirAjustes: () -> Unit, alTocarMetrica: () -> Unit) {
    val marca = SigtecTheme.marca
    // La cabecera llega detrás de la barra de estado; su contenido empieza debajo
    CabeceraHero(Modifier) {
        Row(
            Modifier.statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)
        ) {
            // RF-I2: el avatar abre los ajustes
            Box(
                Modifier
                    .size(AreaTactilMinima.coerceAtLeast(TamanoAvatar))
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable(role = Role.Button, onClickLabel = "Abrir ajustes de seguridad", onClick = alAbrirAjustes),
                contentAlignment = Alignment.Center
            ) {
                Avatar(estado.nombre.ifBlank { "?" }, tamano = TamanoAvatar, esUsuarioActual = true)
            }
            Column(Modifier.weight(1f)) {
                if (estado.cargo.isNotBlank()) {
                    Text(estado.cargo, style = MaterialTheme.typography.bodyMedium, color = marca.sobreHeroTenue)
                }
                Text(
                    if (estado.primerNombre.isBlank()) estado.saludo else "${estado.saludo}, ${estado.primerNombre}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = marca.sobreHero
                )
            }
        }
        estado.resumen?.let { resumen ->
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                resumen.metricas.forEach { metrica ->
                    TarjetaMetrica(
                        valor = "${metrica.valor}",
                        etiqueta = etiqueta(metrica.tipo),
                        variante = variante(metrica, resumen.vista),
                        enCabecera = true,
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.large)
                            .clickable(role = Role.Button, onClick = alTocarMetrica)
                    )
                }
            }
        }
    }
}

private fun etiqueta(tipo: TipoMetrica) = when (tipo) {
    TipoMetrica.ASIGNADAS -> "Asignadas"
    TipoMetrica.EN_ATENCION -> "En atención"
    TipoMetrica.ESPERANDO_REPUESTO -> "Esperando repuesto"
    TipoMetrica.SIN_ASIGNAR -> "Sin asignar"
    TipoMetrica.CRITICAS -> "Críticas"
    TipoMetrica.PENDIENTES -> "Pendientes"
    TipoMetrica.SOLUCIONADAS -> "Solucionadas"
}

/** RF-I3: la que pide acción va en acento; las críticas, en rojo solo si hay alguna. */
private fun variante(metrica: Metrica, vista: VistaInicio): VarianteMetrica = when {
    metrica.tipo == TipoMetrica.CRITICAS && metrica.valor > 0 -> VarianteMetrica.CRITICA
    vista == VistaInicio.TECNICO && metrica.tipo == TipoMetrica.EN_ATENCION -> VarianteMetrica.ACENTO
    vista == VistaInicio.JEFE && metrica.tipo == TipoMetrica.SIN_ASIGNAR -> VarianteMetrica.ACENTO
    vista == VistaInicio.REGISTRADOR && metrica.tipo == TipoMetrica.PENDIENTES -> VarianteMetrica.ACENTO
    else -> VarianteMetrica.NEUTRA
}

/** RF-I7 */
@Composable
private fun AtendiendoAhora(incidencia: Incidencia, ahora: Instant, alContinuar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
        EncabezadoSeccion("Atendiendo ahora")
        Tarjeta(onClick = alContinuar) {
            Column(verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Text(
                    "${incidencia.codigoParaMostrar()} - ${incidencia.codigoEquipo}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    incidencia.descripcion,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    incidencia.responsableConArea(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EtiquetaIncidencia(incidencia.marca, incidencia.critica, incidencia.diasPendiente(ahora))
                    Box(Modifier.weight(1f))
                    BotonTexto(onClick = alContinuar) {
                        Text("Continuar")
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

/** RF-I9: solo lo que ya funciona (decisión I-2). */
@Composable
private fun AccesosRapidos(alRegistrar: () -> Unit, alEscanear: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.l)) {
        AccesoRapido("Registrar", Icons.Outlined.Add, principal = true, alPulsar = alRegistrar)
        AccesoRapido("Escanear", Icons.Outlined.CropFree, principal = false, alPulsar = alEscanear)
    }
}

@Composable
private fun AccesoRapido(texto: String, icono: ImageVector, principal: Boolean, alPulsar: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = alPulsar)
            .padding(Espaciado.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espaciado.s)
    ) {
        Surface(
            color = if (principal) c.primary else c.surface,
            contentColor = if (principal) c.onPrimary else c.primary,
            shape = MaterialTheme.shapes.large,
            shadowElevation = if (principal) 0.dp else 2.dp,
            modifier = Modifier.size(TamanoAcceso)
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icono, contentDescription = null, modifier = Modifier.size(28.dp)) }
        }
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
    }
}

/** RF-I8 */
@Composable
private fun TarjetaRepuesto(repuesto: RepuestoProximo, alAbrir: () -> Unit) {
    val violeta = SigtecTheme.estados.esperandoRepuesto
    Tarjeta(onClick = alAbrir) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            Box(
                Modifier
                    .size(AreaTactilMinima)
                    .clip(MaterialTheme.shapes.small)
                    .background(violeta.contenedor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = violeta.punto)
            }
            Column(Modifier.weight(1f)) {
                Text(repuesto.pieza, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(repuesto.codigoIncidencia, textoEstadoSolicitud(repuesto.estado).lowercase()).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- Vistas previas ----------

private val ahoraDePrueba = Instant.parse("2026-11-08T14:00:00Z")

private val incidenciaDePrueba = Incidencia(
    id = "i124", numero = 124, codigoEquipo = "PC-CONT-014", descripcion = "No enciende después del corte de luz",
    usuarioResponsable = "María Quispe", areaResponsable = "Contabilidad", fechaRegistro = Instant.parse("2026-11-08T09:15:00Z"),
    registradoPorId = "demo-pcardenas", registradoPorNombre = "Pedro Cárdenas", marca = MarcaSeguimiento.EN_ATENCION,
    tecnicoAsignadoId = "demo-lramirez", tecnicoAsignadoNombre = "Luis Ramírez"
)

private fun estadoTecnico() = InicioUiState(
    cargando = false, nombre = "Luis Ramírez", primerNombre = "Luis", cargo = "Técnico de soporte", saludo = "Buenos días",
    resumen = ResumenInicio(
        vista = VistaInicio.TECNICO,
        metricas = listOf(Metrica(TipoMetrica.ASIGNADAS, 3), Metrica(TipoMetrica.EN_ATENCION, 1), Metrica(TipoMetrica.ESPERANDO_REPUESTO, 1)),
        atendiendo = incidenciaDePrueba,
        repuesto = RepuestoProximo("Cable HDMI", "INC-0116", EstadoSolicitudVisible.EN_CAMINO),
        puedeRegistrar = true
    )
)

private fun estadoJefe() = InicioUiState(
    cargando = false, nombre = "Carlos Mendoza", primerNombre = "Carlos", cargo = "Jefe del área de Soporte y Sistemas",
    saludo = "Buenas tardes",
    resumen = ResumenInicio(
        vista = VistaInicio.JEFE,
        metricas = listOf(Metrica(TipoMetrica.SIN_ASIGNAR, 2), Metrica(TipoMetrica.EN_ATENCION, 4), Metrica(TipoMetrica.CRITICAS, 1)),
        puedeRegistrar = true
    )
)

@Composable
private fun Previa(estado: InicioUiState) = Surface(color = MaterialTheme.colorScheme.background) {
    InicioScreen(estado, ahoraDePrueba, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Técnico, claro", showBackground = true, heightDp = 900)
@Composable
private fun InicioTecnicoClaro() = SigtecTheme { Previa(estadoTecnico()) }

@Preview(name = "Técnico, oscuro", showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InicioTecnicoOscuro() = SigtecTheme(darkTheme = true) { Previa(estadoTecnico()) }

@Preview(name = "Jefe, claro", showBackground = true, heightDp = 700)
@Composable
private fun InicioJefeClaro() = SigtecTheme { Previa(estadoJefe()) }

@Preview(name = "Jefe, oscuro", showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InicioJefeOscuro() = SigtecTheme(darkTheme = true) { Previa(estadoJefe()) }
