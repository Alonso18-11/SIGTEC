package com.softcorp.sigtec.feature.captura.domain

import javax.inject.Inject

/**
 * HU-09: del texto que reconoció la cámara al código que se ofrece al usuario (RF-902 a RF-904, RF-907).
 * No verifica permisos: leer no cambia datos ni muestra nada que la persona no tenga delante (plan, P-1).
 */
class LeerCodigoEquipo @Inject constructor(private val catalogo: CatalogoEquiposRepository) {

    suspend operator fun invoke(texto: String): LecturaCodigo? {
        val candidatos = candidatosCodigo(texto)
        if (candidatos.isEmpty()) return null
        // Una etiqueta trae uno o dos candidatos: se consultan todos para preferir el del catálogo (RF-904)
        val delCatalogo = candidatos.filter { catalogo.existe(it) }.toSet()
        return elegirCodigo(candidatos, delCatalogo)
    }
}
