package com.softcorp.sigtec.feature.captura.presentation

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.softcorp.sigtec.core.domain.model.Evidencia
import com.softcorp.sigtec.core.domain.model.TipoEvidencia
import com.softcorp.sigtec.core.theme.Espaciado
import com.softcorp.sigtec.core.theme.SigtecTheme
import com.softcorp.sigtec.core.ui.components.BotonSecundario
import com.softcorp.sigtec.core.ui.components.Seccion
import java.time.Instant

/**
 * HU-09: la sección «Evidencias» con las fotos que la incidencia tiene en este celular (RF-932, prototipo 5).
 * La usan el detalle y el informe técnico (P-7, D-6). «Agregar foto» solo se ve con [puedeAgregar]: el técnico
 * asignado mientras siga pendiente (RF-923). Las fotos las ve cualquiera que abra la incidencia aquí.
 */
@Composable
fun SeccionEvidencias(
    evidencias: List<Evidencia>,
    puedeAgregar: Boolean,
    alAgregarFoto: () -> Unit,
    alAbrirFoto: (Evidencia) -> Unit,
    modifier: Modifier = Modifier
) {
    Seccion("Evidencias", modifier) {
        if (evidencias.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Espaciado.s),
                verticalArrangement = Arrangement.spacedBy(Espaciado.s)
            ) {
                evidencias.forEach { foto -> FotoDeLaSeccion(foto, alAbrir = { alAbrirFoto(foto) }) }
            }
        }
        if (puedeAgregar) {
            BotonSecundario(onClick = alAgregarFoto) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(TamanoIcono))
                Spacer(Modifier.width(Espaciado.s))
                Text("Agregar foto")
            }
        }
        // RF-933: las fotos no se sincronizan, así que en otro celular la incidencia aparece sin ellas
        if (evidencias.isEmpty()) Nota("Sin fotos en este celular.")
        Nota("Las fotos se guardan solo en el celular que las tomó.")
    }
}

/** RF-934: tocarla abre la foto ampliada. */
@Composable
private fun FotoDeLaSeccion(foto: Evidencia, alAbrir: () -> Unit) {
    val rotulo = rotuloDeEvidencia(foto.tipo)
    Column(
        modifier = Modifier
            .width(LadoFoto)
            .clickable(onClickLabel = "Ver foto", onClick = alAbrir),
        verticalArrangement = Arrangement.spacedBy(Espaciado.xs)
    ) {
        MiniaturaEvidencia(foto.rutaArchivo, LadoFoto, descripcion = rotulo)
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Nota(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** RF-932: «Foto de la falla» o «Foto de la pieza cambiada». */
fun rotuloDeEvidencia(tipo: TipoEvidencia): String = when (tipo) {
    TipoEvidencia.FALLA -> "Foto de la falla"
    TipoEvidencia.PIEZA_CAMBIADA -> "Foto de la pieza cambiada"
}

private val LadoFoto = 96.dp
private val TamanoIcono = 18.dp

// ---------- Vistas previas ----------

private val fotosDePrueba = listOf(
    Evidencia("a", "i124", TipoEvidencia.FALLA, "/no-existe/a.jpg", Instant.parse("2026-10-18T15:00:00Z")),
    Evidencia("b", "i124", TipoEvidencia.PIEZA_CAMBIADA, "/no-existe/b.jpg", Instant.parse("2026-10-18T15:05:00Z"))
)

@Preview(name = "Con fotos, técnico asignado · claro", showBackground = true)
@Composable
private fun ConFotosClaro() = SigtecTheme {
    Surface { SeccionEvidencias(fotosDePrueba, puedeAgregar = true, {}, {}, Modifier.padding(Espaciado.m)) }
}

@Preview(name = "Con fotos, otro perfil · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConFotosOscuro() = SigtecTheme(darkTheme = true) {
    Surface { SeccionEvidencias(fotosDePrueba, puedeAgregar = false, {}, {}, Modifier.padding(Espaciado.m)) }
}

@Preview(name = "Sin fotos · claro", showBackground = true)
@Composable
private fun SinFotosClaro() = SigtecTheme {
    Surface { SeccionEvidencias(emptyList(), puedeAgregar = true, {}, {}, Modifier.padding(Espaciado.m)) }
}

@Preview(name = "Sin fotos · oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SinFotosOscuro() = SigtecTheme(darkTheme = true) {
    Surface { SeccionEvidencias(emptyList(), puedeAgregar = false, {}, {}, Modifier.padding(Espaciado.m)) }
}
