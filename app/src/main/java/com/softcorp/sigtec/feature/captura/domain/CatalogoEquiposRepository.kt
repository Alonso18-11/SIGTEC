package com.softcorp.sigtec.feature.captura.domain

/** El catálogo de equipos que ya bajó a este celular; el escaneo solo lo consulta (RF-904, RF-907). */
interface CatalogoEquiposRepository {
    /** True si el código está en el catálogo sincronizado localmente. */
    suspend fun existe(codigo: String): Boolean
}
