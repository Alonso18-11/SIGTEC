package com.softcorp.sigtec.feature.acceso.presentation

/** Singular con 1, plural con el resto. Mismo sustantivo que `AvisoConexion`. */ // RF-11, RF-13
fun textoCambiosSinEnviar(n: Int): String =
    if (n == 1) "1 cambio sin enviar" else "$n cambios sin enviar"

/** El verbo concuerda con el número de cambios: «se perderá» con 1, «se perderán» con el resto. */ // RF-11, RF-13
fun textoSePerdera(n: Int): String =
    if (n == 1) "se perderá" else "se perderán"
