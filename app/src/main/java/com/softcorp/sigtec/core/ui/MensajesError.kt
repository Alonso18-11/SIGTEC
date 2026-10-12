package com.softcorp.sigtec.core.ui

import com.softcorp.sigtec.core.domain.ErrorDominio

fun ErrorDominio.mensaje(): String = when (this) {
    ErrorDominio.CredencialesInvalidas -> "Correo o contraseña incorrectos."
    ErrorDominio.SinConexion -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    ErrorDominio.DemasiadosIntentos -> "Demasiados intentos. Espera unos minutos."
    ErrorDominio.PerfilNoConfigurado -> "Tu cuenta no tiene un perfil asignado. Consulta al jefe del área."
    ErrorDominio.SinPermiso -> "No tienes permiso para esta acción."
    ErrorDominio.CampoObligatorio -> "Completa todos los campos obligatorios."
    ErrorDominio.ConfirmacionRequerida -> "Debes confirmar con tu huella."
    ErrorDominio.EquipoNoEncontrado -> "El código de equipo no existe en el catálogo. Verifícalo e inténtalo de nuevo."
    ErrorDominio.IncidenciaCerrada -> "Esta incidencia ya está cerrada; no se puede asignar."
    ErrorDominio.IncidenciaPorSincronizar ->
        "Esta incidencia aún no se sincroniza. Podrás asignarla cuando tenga su número INC."
    is ErrorDominio.YaAsignada -> "La incidencia ya está asignada a $tecnicoNombre."
    is ErrorDominio.LimiteAtencionAlcanzado -> "$tecnicoNombre alcanzó su límite de atención."
    is ErrorDominio.Desconocido -> "Ocurrió un error inesperado. Inténtalo de nuevo."
}