package com.softcorp.sigtec.core.domain.model

enum class Permiso {
    REGISTRAR_INCIDENCIA,
    CONSULTAR_INCIDENCIAS,
    ASIGNAR_INCIDENCIA,
    ATENDER_INCIDENCIA,
    REGISTRAR_FALLA,
    CONSULTAR_DICCIONARIO,
    VER_INDICADORES,
    CONSULTAR_HISTORIAL
}

enum class Perfil(val permisos: Set<Permiso>) {
    JEFE(
        setOf(
            Permiso.REGISTRAR_INCIDENCIA, Permiso.CONSULTAR_INCIDENCIAS,
            Permiso.ASIGNAR_INCIDENCIA, Permiso.VER_INDICADORES,
            Permiso.CONSULTAR_HISTORIAL
        )
    ),
    TECNICO(
        setOf(
            Permiso.REGISTRAR_INCIDENCIA, Permiso.CONSULTAR_INCIDENCIAS,
            Permiso.ATENDER_INCIDENCIA, Permiso.REGISTRAR_FALLA,
            Permiso.CONSULTAR_DICCIONARIO, Permiso.CONSULTAR_HISTORIAL
        )
    ),
    PERSONAL_SISTEMAS(
        setOf(
            Permiso.REGISTRAR_INCIDENCIA, Permiso.CONSULTAR_INCIDENCIAS,
            Permiso.CONSULTAR_DICCIONARIO, Permiso.CONSULTAR_HISTORIAL
        )
    );

    fun puede(permiso: Permiso): Boolean = permiso in permisos
}