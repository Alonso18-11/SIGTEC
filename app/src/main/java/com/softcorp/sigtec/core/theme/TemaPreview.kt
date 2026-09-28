package com.softcorp.sigtec.core.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.softcorp.sigtec.core.ui.componentes.EtiquetaEstado

@Composable
private fun Muestrario() {
    Surface {
        Column(Modifier.padding(Espaciado.m), verticalArrangement = Arrangement.spacedBy(Espaciado.s)) {
            Text("Indicadores del área", style = MaterialTheme.typography.titleLarge)
            Text("Texto de cuerpo", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = {}) { Text("Ingresar") }
            OutlinedButton(onClick = {}) { Text("Ingresar con huella") }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text("INC-0124 · Atendiendo ahora", Modifier.padding(Espaciado.m))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                EtiquetaEstado("Sin asignar", SigtecTheme.estados.sinAsignar)
                EtiquetaEstado("En atención", SigtecTheme.estados.enAtencion)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espaciado.s)) {
                EtiquetaEstado("Esperando repuesto", SigtecTheme.estados.esperandoRepuesto)
                EtiquetaEstado("Solucionado", SigtecTheme.estados.solucionado)
            }
            EtiquetaEstado("Caso crítico · 6 días", SigtecTheme.estados.critico)
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Composable
private fun MuestrarioClaro() = SigtecTheme { Muestrario() }

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MuestrarioOscuro() = SigtecTheme(darkTheme = true) { Muestrario() }