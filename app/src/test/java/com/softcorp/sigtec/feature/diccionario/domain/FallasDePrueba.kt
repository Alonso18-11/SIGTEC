package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.core.domain.model.FallaDiccionario
import com.softcorp.sigtec.core.domain.model.TipoFalla
import java.time.Instant

/** Las fallas del prototipo 14, para las pruebas del diccionario. */
internal object FallasDePrueba {
    private val fecha = Instant.parse("2026-11-01T10:00:00Z")

    fun falla(
        id: String,
        titulo: String,
        componente: String,
        sintomas: String,
        tipo: TipoFalla = TipoFalla.HARDWARE,
        solucion: String = "Solución de $titulo",
        origen: String? = null
    ) = FallaDiccionario(
        id = id, titulo = titulo, sintomas = sintomas, componente = componente, tipo = tipo, solucion = solucion,
        autorId = "demo-lramirez", autorNombre = "Luis Ramírez", registradaEn = fecha, incidenciaOrigenId = origen
    )

    val fuente = falla(
        "f1", "Fuente de poder dañada por variación de voltaje", "Fuente de poder",
        "La PC no enciende, no prende ninguna luz después de un corte de luz",
        solucion = "Reemplazar la fuente y revisar el estabilizador."
    )
    val impresora = falla(
        "f2", "La impresora no aparece en la red", "Impresora",
        "No se puede imprimir desde las PC del área, la impresora no responde al ping", TipoFalla.RED,
        solucion = "Asignar una IP fija y reiniciar la cola de impresión."
    )
    val pantallaAzul = falla(
        "f3", "Pantalla azul al iniciar Windows", "Sistema operativo",
        "Se reinicia con pantalla azul después de instalar un controlador", TipoFalla.SOFTWARE,
        solucion = "Iniciar en modo seguro y revertir el último controlador instalado."
    )
    val office = falla(
        "f4", "Office pide activar la licencia", "Office",
        "Word y Excel muestran producto sin licencia y no dejan editar", TipoFalla.SOFTWARE,
        solucion = "Volver a vincular la cuenta institucional."
    )

    val todas = listOf(fuente, impresora, pantallaAzul, office)
}
