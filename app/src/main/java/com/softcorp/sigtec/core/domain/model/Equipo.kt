package com.softcorp.sigtec.core.domain.model

data class Equipo(
    val codigo: String,              // PC-CONT-014
    val tipo: String,                // Computadora de escritorio, laptop, impresora
    val marca: String,
    val modelo: String,
    val oficina: String,
    val responsableNombre: String
)