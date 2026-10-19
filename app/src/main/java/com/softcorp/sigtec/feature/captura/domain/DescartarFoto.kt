package com.softcorp.sigtec.feature.captura.domain

import javax.inject.Inject

/**
 * HU-09 (RF-928): borra el archivo de una foto que no llegó a enlazarse, porque la cámara falló o la escribió
 * a medias. No verifica permisos: solo borra un archivo que nadie ve, dentro de la carpeta de fotos.
 */
class DescartarFoto @Inject constructor(
    private val repo: EvidenciaRepository
) {
    suspend operator fun invoke(foto: FotoNueva) = repo.descartarArchivo(foto.ruta)
}
