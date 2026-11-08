package com.softcorp.sigtec.core.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.ui.components.BotonPrincipal
import com.softcorp.sigtec.core.ui.components.BotonSecundario
import com.softcorp.sigtec.core.ui.components.EncabezadoSeccion
import com.softcorp.sigtec.core.ui.components.EtiquetaEstado

// Muestrario del tema v2: escala tipográfica, roles principales, marca y estados, en claro y oscuro.

@Composable
private fun Muestra(nombre: String, fondo: Color, texto: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(fondo)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .padding(Espaciado.s)
    ) {
        Text(nombre, style = MaterialTheme.typography.labelMedium, color = texto)
    }
}

@Composable
private fun Muestrario() {
    val c = MaterialTheme.colorScheme
    val t = MaterialTheme.typography
    val marca = SigtecTheme.marca
    Surface(color = c.background) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.m),
            verticalArrangement = Arrangement.spacedBy(Espaciado.s)
        ) {
            Text("Incidencias", style = t.headlineLarge)
            Text("Título de cabecera", style = t.headlineMedium)
            Text("Barra de formulario", style = t.titleLarge)
            Text("Título de sección", style = t.titleMedium)
            Text("Título de tarjeta", style = t.titleSmall)
            Text("Texto de campo", style = t.bodyLarge)
            Text("Texto de tarjeta", style = t.bodyMedium)
            Text("Dato secundario", style = t.bodySmall, color = c.onSurfaceVariant)
            Text("INC-0124 - PC-CONT-014", style = t.labelMedium, color = c.onSurfaceVariant)

            EncabezadoSeccion("Roles")
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Muestra("primary", c.primary, c.onPrimary)
                Muestra("primaryContainer", c.primaryContainer, c.onPrimaryContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Muestra("surface", c.surface, c.onSurface)
                Muestra("tertiaryContainer", c.tertiaryContainer, c.onTertiaryContainer)
                Muestra("errorContainer", c.errorContainer, c.onErrorContainer)
            }

            EncabezadoSeccion("Marca")
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                Box(
                    Modifier
                        .clip(FormaCabecera)
                        .background(marca.hero)
                        .padding(Espaciado.m)
                ) {
                    Column {
                        Text("Técnico de soporte", style = t.bodySmall, color = marca.sobreHeroTenue)
                        Text("Buenos días, Luis", style = t.titleMedium, color = marca.sobreHero)
                    }
                }
                Box(
                    Modifier
                        .clip(FormaCompleta)
                        .background(marca.acento)
                        .padding(horizontal = Espaciado.m, vertical = Espaciado.sm)
                ) {
                    Text("Nueva", style = t.labelLarge, color = marca.sobreAcento)
                }
            }

            EncabezadoSeccion("Estados")
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                EtiquetaEstado("Sin asignar", SigtecTheme.estados.sinAsignar)
                EtiquetaEstado("Asignada", SigtecTheme.estados.asignada)
                EtiquetaEstado("En atención", SigtecTheme.estados.enAtencion)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                EtiquetaEstado("Esperando repuesto", SigtecTheme.estados.esperandoRepuesto)
                EtiquetaEstado("Solucionado", SigtecTheme.estados.solucionado)
            }
            EtiquetaEstado("Crítico, 6 días", SigtecTheme.estados.critico)

            EncabezadoSeccion("Acciones")
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                BotonPrincipal(onClick = {}) { Text("Ingresar") }
                BotonSecundario(onClick = {}) { Text("Ingresar con huella") }
            }
            OutlinedTextField(value = "PC-CONT-014", onValueChange = {}, label = { Text("Código del equipo") })
        }
    }
}

@Preview(name = "Claro", showBackground = true, heightDp = 1100)
@Composable
private fun MuestrarioClaro() = SigtecTheme { Muestrario() }

@Preview(name = "Oscuro", showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MuestrarioOscuro() = SigtecTheme(darkTheme = true) { Muestrario() }
