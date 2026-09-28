package com.softcorp.sigtec.core.ui

import com.softcorp.sigtec.core.domain.ErrorDominio

// El "when" es exhaustivo: si alguien agrega un ErrorDominio, esto no compila
// hasta que le escriba su mensaje. Ningún error llega a la pantalla sin texto.
fun ErrorDominio.mensaje(): String = when (this) {
    ErrorDominio.CredencialesInvalidas -> "Correo o contraseña incorrectos."
    ErrorDominio.SinConexion -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    ErrorDominio.DemasiadosIntentos -> "Demasiados intentos. Espera unos minutos."
    ErrorDominio.PerfilNoConfigurado -> "Tu cuenta no tiene un perfil asignado. Consulta al jefe del área."
    ErrorDominio.SinPermiso -> "No tienes permiso para esta acción."
    ErrorDominio.ConfirmacionRequerida -> "Debes confirmar con tu huella."
    is ErrorDominio.LimiteAtencionAlcanzado -> "$tecnicoNombre alcanzó su límite de atención."
    is ErrorDominio.Desconocido -> "Ocurrió un error inesperado. Inténtalo de nuevo."
}