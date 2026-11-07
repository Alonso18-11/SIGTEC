package com.softcorp.sigtec.feature.acceso.presentation

/** Singular con 1, plural con el resto. Mismo sustantivo que `AvisoConexion`. */ // RF-11, RF-13
fun textoCambiosSinEnviar(n: Int): String =
    if (n == 1) "1 cambio sin enviar" else "$n cambios sin enviar"

/** El verbo concuerda con el número de cambios: «se perderá» con 1, «se perderán» con el resto. */ // RF-11, RF-13
fun textoSePerdera(n: Int): String =
    if (n == 1) "se perderá" else "se perderán"

/** Singular con 1, plural con el resto. */ // RF-943, RF-944
fun textoFotosEvidencia(n: Int): String =
    if (n == 1) "1 foto de evidencia" else "$n fotos de evidencia"

/** Aviso de «Cerrar sesión»; sin fotos queda igual que el texto de RF-13. */ // RF-13, RF-943
fun textoAvisoSalida(pendientes: Int, fotos: Int): String {
    val (loQueHay, verbo) = loQueSePierde(pendientes, fotos)
    val dondeEsta = if (fotos > 0) " en este celular" else ""
    return "Tienes $loQueHay$dondeEsta. Si cierras sesión ahora, $verbo."
}

/** Aviso al entrar otra persona con el dueño bloqueado. */ // RF-11, RF-944
fun textoAvisoIngreso(nombre: String, pendientes: Int, fotos: Int): String {
    val (loQueHay, verbo) = loQueSePierde(pendientes, fotos)
    return "$nombre tiene $loQueHay en este celular. Si ingresas, $verbo."
}

// Con cambios y fotos, el verbo va siempre en plural: son dos cosas.
private fun loQueSePierde(pendientes: Int, fotos: Int): Pair<String, String> = when {
    pendientes > 0 && fotos > 0 ->
        "${textoCambiosSinEnviar(pendientes)} y ${textoFotosEvidencia(fotos)}" to "se perderán"
    fotos > 0 -> textoFotosEvidencia(fotos) to textoSePerdera(fotos)
    else -> textoCambiosSinEnviar(pendientes) to textoSePerdera(pendientes)
}
