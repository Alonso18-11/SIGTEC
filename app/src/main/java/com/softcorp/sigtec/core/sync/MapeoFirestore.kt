package com.softcorp.sigtec.core.sync

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.softcorp.sigtec.core.database.entity.EquipoEntity
import com.softcorp.sigtec.core.database.entity.IncidenciaEntity
import com.softcorp.sigtec.core.database.entity.InformeTecnicoEntity
import com.softcorp.sigtec.core.database.entity.MovimientoEntity
import com.softcorp.sigtec.core.database.entity.TecnicoEntity
import com.softcorp.sigtec.core.domain.model.MarcaSeguimiento
import com.softcorp.sigtec.core.domain.model.Prioridad
import com.softcorp.sigtec.core.domain.model.TipoFalla
import com.softcorp.sigtec.core.domain.model.TipoMovimiento
import java.time.Instant

private fun Instant.aTimestamp() = Timestamp(this)


internal fun IncidenciaEntity.aFirestore(numero: Int): Map<String, Any?> = mapOf(
    "numero" to numero,
    "codigoEquipo" to codigoEquipo,
    "descripcion" to descripcion,
    "usuarioResponsable" to usuarioResponsable,
    "fechaRegistro" to fechaRegistro.aTimestamp(),
    "registradoPorId" to registradoPorId,
    "registradoPorNombre" to registradoPorNombre,
    "marca" to marca.name,
    "tipoFalla" to tipoFalla?.name,
    "prioridad" to prioridad?.name,
    "tecnicoAsignadoId" to tecnicoAsignadoId,
    "tecnicoAsignadoNombre" to tecnicoAsignadoNombre,
    "critica" to critica,
    "actualizadoEn" to actualizadoEn.aTimestamp()
)

internal fun DocumentSnapshot.aIncidenciaEntity(): IncidenciaEntity? = runCatching {
    IncidenciaEntity(
        id = id,
        numero = getLong("numero")?.toInt(),
        codigoEquipo = getString("codigoEquipo")!!,
        descripcion = getString("descripcion")!!,
        usuarioResponsable = getString("usuarioResponsable")!!,
        fechaRegistro = getTimestamp("fechaRegistro")!!.toInstant(),
        registradoPorId = getString("registradoPorId")!!,
        registradoPorNombre = getString("registradoPorNombre")!!,
        marca = MarcaSeguimiento.valueOf(getString("marca")!!),
        tipoFalla = getString("tipoFalla")?.let(TipoFalla::valueOf),
        prioridad = getString("prioridad")?.let(Prioridad::valueOf),
        tecnicoAsignadoId = getString("tecnicoAsignadoId"),
        tecnicoAsignadoNombre = getString("tecnicoAsignadoNombre"),
        critica = getBoolean("critica") ?: false,
        pendienteSincronizar = false,
        actualizadoEn = getTimestamp("actualizadoEn")?.toInstant() ?: Instant.EPOCH
    )
}.getOrNull()

// ---------- Movimientos ----------

internal fun MovimientoEntity.aFirestore(): Map<String, Any?> = mapOf(
    "incidenciaId" to incidenciaId,
    "tipo" to tipo.name,
    "autorNombre" to autorNombre,
    "fecha" to fecha.aTimestamp(),
    "detalle" to detalle
)

internal fun DocumentSnapshot.aMovimientoEntity(): MovimientoEntity? = runCatching {
    MovimientoEntity(
        id = id,
        incidenciaId = getString("incidenciaId")!!,
        tipo = TipoMovimiento.valueOf(getString("tipo")!!),
        autorNombre = getString("autorNombre")!!,
        fecha = getTimestamp("fecha")!!.toInstant(),
        detalle = getString("detalle"),
        pendienteSincronizar = false
    )
}.getOrNull()

// ---------- Informes técnicos ----------

internal fun InformeTecnicoEntity.aFirestore(): Map<String, Any?> = mapOf(
    "diagnostico" to diagnostico,
    "trabajoRealizado" to trabajoRealizado,
    "piezasCambiadas" to piezasCambiadas,
    "observaciones" to observaciones,
    "fechaCierre" to fechaCierre.aTimestamp(),
    "confirmadoEn" to confirmadoEn.aTimestamp()
)

// ---------- Catálogo ----------

internal fun DocumentSnapshot.aEquipoEntity(): EquipoEntity? = runCatching {
    EquipoEntity(
        codigo = id,
        tipo = getString("tipo")!!,
        marca = getString("marca")!!,
        modelo = getString("modelo")!!,
        oficina = getString("oficina")!!,
        responsableNombre = getString("responsableNombre")!!
    )
}.getOrNull()

/**
 * RF-619 (HU-06): un usuario técnico de /usuarios, cuyo id es su UID. Trabaja sobre el Map del
 * documento para probarse en la JVM sin clases de Firebase. Los enteros llegan como Long (int64).
 * Si falta el nombre o el límite, el documento se descarta (como RF-60).
 */
internal fun tecnicoDesdeDatos(id: String, datos: Map<String, Any?>): TecnicoEntity? {
    val nombre = datos["nombre"] as? String ?: return null
    val limite = datos["limiteAtencion"] as? Long ?: return null
    // Sin carga registrada todavía, no tiene incidencias activas
    val activas = datos["incidenciasActivas"] as? Long ?: 0L
    return TecnicoEntity(id, nombre, limiteAtencion = limite.toInt(), incidenciasActivas = activas.toInt())
}

internal fun DocumentSnapshot.aTecnicoEntity(): TecnicoEntity? = data?.let { tecnicoDesdeDatos(id, it) }
