package com.softcorp.sigtec.feature.captura.data

import com.softcorp.sigtec.feature.captura.domain.CatalogoEquiposRepository

/** El catálogo en memoria, por defecto con los 4 equipos de prueba que hay en Firestore. */
class CatalogoEquiposRepositoryFalso(
    private val codigos: Set<String> = setOf("PC-CONT-014", "PC-LOG-009", "PC-SIS-003", "IMP-RRHH-002")
) : CatalogoEquiposRepository {

    /** Códigos consultados, en orden, para comprobar qué preguntó el caso de uso. */
    val consultados = mutableListOf<String>()

    override suspend fun existe(codigo: String): Boolean {
        consultados += codigo
        return codigo in codigos
    }
}
