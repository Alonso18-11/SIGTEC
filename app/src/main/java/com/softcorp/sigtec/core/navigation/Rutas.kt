package com.softcorp.sigtec.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Ruta {
    // Acceso
    @Serializable data object Login : Ruta
    @Serializable data object AjustesSeguridad : Ruta
    // Inicio
    @Serializable data object Inicio : Ruta
    // Incidencias
    @Serializable data object ListaIncidencias : Ruta
    @Serializable data object RegistroIncidencia : Ruta
    @Serializable data class DetalleIncidencia(val incidenciaId: String) : Ruta
    // Asignación
    @Serializable data class AsignarIncidencia(val incidenciaId: String) : Ruta
    // Atención
    @Serializable data object MisTareas : Ruta
    @Serializable data class SolicitarRepuesto(val incidenciaId: String) : Ruta
    @Serializable data class RegistrarInforme(val incidenciaId: String) : Ruta
    @Serializable data object EstadoRepuestos : Ruta
    // Captura (recursos del celular)
    @Serializable data object EscanearEtiqueta : Ruta
    @Serializable data class CapturarEvidencia(val incidenciaId: String) : Ruta
    @Serializable data object DictarTexto : Ruta
    // Conocimiento e IA
    @Serializable data object Diccionario : Ruta
    @Serializable data class Asistente(val incidenciaId: String) : Ruta
    // Control del área
    @Serializable data object Indicadores : Ruta
    @Serializable data class HistorialEquipo(val codigoEquipo: String? = null) : Ruta
}