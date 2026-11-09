package com.softcorp.sigtec.feature.indicadores.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.CabeceraHero
import com.softcorp.sigtec.core.ui.components.FondoBarraEstado
import com.softcorp.sigtec.core.ui.components.IconosBarraEstadoClaros
import com.softcorp.sigtec.core.ui.components.Tarjeta
import com.softcorp.sigtec.core.ui.components.TarjetaMetrica
import com.softcorp.sigtec.core.ui.components.VarianteMetrica
import com.softcorp.sigtec.feature.indicadores.domain.CargaTecnico
import com.softcorp.sigtec.feature.indicadores.domain.ConteoEstado
import com.softcorp.sigtec.feature.indicadores.domain.EquipoAEvaluar
import com.softcorp.sigtec.feature.indicadores.domain.Indicadores
import com.softcorp.sigtec.feature.indicadores.domain.ReparadosDia
import java.time.LocalDate

private val AltoBarra = 10.dp
private val AnchoNombreTecnico = 88.dp
private val TamanoIconoEquipo = 48.dp

@Composable
fun IndicadoresRoute(
    alAbrirEquipo: (String) -> Unit,
    vm: IndicadoresViewModel = hiltViewModel()
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    IndicadoresScreen(estado, alAbrirEquipo)
}

/** HU-15 (prototipo 10): cabecera con métricas, reparados por día, estados, carga y equipo a evaluar. Solo dibuja. */
@Composable
fun IndicadoresScreen(estado: IndicadoresUiState, alAbrirEquipo: (String) -> Unit) {
    val desplazamiento = rememberScrollState()
    val desplazado by remember { derivedStateOf { desplazamiento.value > 0 } }
    Box(Modifier.fillMaxSize()) {
        when {
            estado.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            estado.sinPermiso -> Text(
                "No tienes acceso a los indicadores.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Espaciado.m)
            )
            else -> {
                IconosBarraEstadoClaros()
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(desplazamiento)
                ) {
                    Cabecera(estado)
                    estado.indicadores?.let { ind ->
                        Column(
                            Modifier.padding(Espaciado.m),
                            verticalArrangement = Arrangement.spacedBy(Espaciado.m)
                        ) {
                            TarjetaReparados(ind)
                            TarjetaEstados(ind.porEstado)
                            TarjetaCarga(ind.cargas)
                            ind.equipoAEvaluar?.let { TarjetaEquipo(it, alAbrirEquipo) }
                        }
                    }
                }
                FondoBarraEstado(visible = desplazado)
            }
        }
    }
}

@Composable
private fun Cabecera(estado: IndicadoresUiState) {
    val marca = SigtecTheme.marca
    CabeceraHero {
        Column(Modifier.statusBarsPadding(), verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            val quien = listOf(estado.nombre, estado.cargo).filter { it.isNotBlank() }.joinToString(", ")
            if (quien.isNotEmpty()) {
                Text(quien, style = MaterialTheme.typography.bodyMedium, color = marca.sobreHeroTenue)
            }
            Text(
                "Indicadores del área",
                style = MaterialTheme.typography.headlineMedium,
                color = marca.sobreHero,
                modifier = Modifier.semantics { heading() }
            )
            Text(estado.actualizado, style = MaterialTheme.typography.bodyMedium, color = marca.sobreHeroTenue)
        }
        estado.indicadores?.let { ind ->
            // RF-1505: las tres a la misma altura aunque una etiqueta ocupe dos líneas
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                val alto = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                TarjetaMetrica("${ind.reparados7Dias}", "Reparados en 7 días", alto, enCabecera = true)
                TarjetaMetrica("${ind.pendientes}", "Pendientes", alto, enCabecera = true)
                // RF-1508: en alerta solo si hay alguno
                TarjetaMetrica(
                    "${ind.criticos}", "Críticos (+5 días)", alto,
                    variante = if (ind.criticos > 0) VarianteMetrica.CRITICA else VarianteMetrica.NEUTRA,
                    enCabecera = true
                )
            }
        }
    }
}

@Composable
private fun TituloTarjeta(texto: String, detalle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
        Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        detalle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** RF-1509 a RF-1514. */
@Composable
private fun TarjetaReparados(ind: Indicadores) {
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            TituloTarjeta("Equipos reparados por día", ind.horasPromedioAtencion?.let(::textoTiempoAtencion))
            GraficoReparados(ind.reparadosPorDia)
            if (ind.reparados7Dias == 0) {
                Text(
                    "Sin equipos reparados en los últimos 7 días",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** RF-1515, RF-1516: una fila por marca con su color, su nombre y su número. */
@Composable
private fun TarjetaEstados(estados: List<ConteoEstado>) {
    val total = estados.sumOf { it.cantidad }
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            TituloTarjeta("Incidencias por estado")
            estados.forEach { e ->
                val color = colorEstado(e.marca)
                FilaBarra(
                    nombre = nombreEstado(e.marca),
                    valor = "${e.cantidad}",
                    fraccion = if (total == 0) 0f else e.cantidad.toFloat() / total,
                    colorBarra = color,
                    anchoNombre = null
                )
            }
        }
    }
}

@Composable
private fun colorEstado(marca: MarcaSeguimiento): Color {
    val e = SigtecTheme.estados
    return when (marca) {
        MarcaSeguimiento.SIN_ASIGNAR -> e.sinAsignar.punto
        MarcaSeguimiento.ASIGNADA -> e.asignada.punto
        MarcaSeguimiento.EN_ATENCION -> e.enAtencion.punto
        MarcaSeguimiento.ESPERANDO_REPUESTO -> e.esperandoRepuesto.punto
        MarcaSeguimiento.CERRADA -> e.solucionado.punto
    }
}

/** RF-1517 a RF-1520. */
@Composable
private fun TarjetaCarga(cargas: List<CargaTecnico>) {
    val c = MaterialTheme.colorScheme
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            TituloTarjeta("Carga por técnico")
            if (cargas.isEmpty()) {
                Text(
                    "Aún no se descargaron los técnicos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
            }
            cargas.forEach { t ->
                FilaBarra(
                    nombre = t.nombre.substringBefore(' '),
                    valor = "${t.activas} / ${t.limite}",
                    fraccion = if (t.limite <= 0) 1f else (t.activas.toFloat() / t.limite).coerceIn(0f, 1f),
                    colorBarra = if (t.alcanzoLimite) c.error else c.primary,
                    colorValor = if (t.alcanzoLimite) c.error else c.onSurface,
                    anchoNombre = AnchoNombreTecnico
                )
            }
        }
    }
}

/** Nombre, barra y número en una fila; con [anchoNombre] null el nombre va arriba de la barra. */
@Composable
private fun FilaBarra(
    nombre: String,
    valor: String,
    fraccion: Float,
    colorBarra: Color,
    anchoNombre: androidx.compose.ui.unit.Dp?,
    colorValor: Color = MaterialTheme.colorScheme.onSurface
) {
    val pista = MaterialTheme.colorScheme.surfaceVariant
    val barra: @Composable (Modifier) -> Unit = { mod ->
        Box(
            mod
                .height(AltoBarra)
                .clip(CircleShape)
                .background(pista)
        ) {
            if (fraccion > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraccion)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(colorBarra)
                )
            }
        }
    }
    if (anchoNombre != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.sm)) {
            Text(nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(anchoNombre), maxLines = 1)
            barra(Modifier.weight(1f))
            Text(valor, style = MaterialTheme.typography.labelLarge, color = colorValor)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Espaciado.xs)) {
            Row {
                Text(nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(valor, style = MaterialTheme.typography.labelLarge, color = colorValor)
            }
            barra(Modifier.fillMaxWidth())
        }
    }
}

/** RF-1521, RF-1522: el equipo con muchas incidencias, que abre su historial. */
@Composable
private fun TarjetaEquipo(equipo: EquipoAEvaluar, alAbrirEquipo: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    Tarjeta(Modifier.fillMaxWidth(), onClick = { alAbrirEquipo(equipo.codigo) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Espaciado.m)) {
            Surface(
                color = c.errorContainer,
                contentColor = c.onErrorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(TamanoIconoEquipo)
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.WarningAmber, contentDescription = null) }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(equipo.codigo, equipo.oficina).joinToString(" - "),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    textoEquipoAEvaluar(equipo.incidencias),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Ver historial del equipo")
        }
    }
}

// ---------- Vistas previas ----------

private val hoy = LocalDate.of(2026, 11, 9)

private val indicadoresDePrueba = Indicadores(
    reparados7Dias = 32,
    pendientes = 14,
    criticos = 3,
    reparadosPorDia = listOf(5, 6, 4, 7, 6, 3, 1).mapIndexed { i, n -> ReparadosDia(hoy.minusDays(6L - i), n, i == 6) },
    horasPromedioAtencion = 43.2,
    porEstado = listOf(
        ConteoEstado(MarcaSeguimiento.SIN_ASIGNAR, 3), ConteoEstado(MarcaSeguimiento.ASIGNADA, 4),
        ConteoEstado(MarcaSeguimiento.EN_ATENCION, 5), ConteoEstado(MarcaSeguimiento.ESPERANDO_REPUESTO, 2),
        ConteoEstado(MarcaSeguimiento.CERRADA, 40)
    ),
    cargas = listOf(
        CargaTecnico("1", "Luis Ramírez", 3, 5), CargaTecnico("2", "Ana Torres", 5, 5),
        CargaTecnico("3", "Jorge Salas", 1, 4), CargaTecnico("4", "Rosa Vilca", 2, 4)
    ),
    equipoAEvaluar = EquipoAEvaluar("PC-CONT-014", "Contabilidad", 4)
)

private val estadoDePrueba = IndicadoresUiState(
    cargando = false, nombre = "Carlos Mendoza", cargo = "Jefe del área",
    actualizado = "Actualizado hoy a las 18:00", indicadores = indicadoresDePrueba
)

@Composable
private fun Previa(estado: IndicadoresUiState) = Surface(color = MaterialTheme.colorScheme.background) {
    IndicadoresScreen(estado, alAbrirEquipo = {})
}

@Preview(name = "Indicadores, claro", showBackground = true, heightDp = 1400)
@Composable
private fun IndicadoresClaro() = SigtecTheme { Previa(estadoDePrueba) }

@Preview(name = "Indicadores, oscuro", showBackground = true, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IndicadoresOscuro() = SigtecTheme(darkTheme = true) { Previa(estadoDePrueba) }

@Preview(name = "Sin cierres ni técnicos, claro", showBackground = true, heightDp = 1100)
@Composable
private fun IndicadoresVacio() = SigtecTheme {
    Previa(
        estadoDePrueba.copy(
            actualizado = "Sin datos del servidor todavía",
            indicadores = indicadoresDePrueba.copy(
                reparados7Dias = 0, criticos = 0, horasPromedioAtencion = null, cargas = emptyList(), equipoAEvaluar = null,
                reparadosPorDia = indicadoresDePrueba.reparadosPorDia.map { it.copy(cantidad = 0) }
            )
        )
    )
}
