package com.softcorp.sigtec.core.domain.repository

import com.softcorp.sigtec.core.domain.model.EstadoDatos
import kotlinx.coroutines.flow.Flow

interface EstadoDatosRepository {
    val estado: Flow<EstadoDatos>
}
