package com.softcorp.sigtec.feature.captura.data

import android.util.Log
import com.softcorp.sigtec.core.database.dao.CatalogoDao
import com.softcorp.sigtec.core.di.IoDispatcher
import com.softcorp.sigtec.feature.captura.domain.CatalogoEquiposRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Solo lee el catálogo que `core/sync` ya bajó a Room; nunca habla con Firestore. */
class CatalogoEquiposRepositoryImpl @Inject constructor(
    private val catalogoDao: CatalogoDao,
    @IoDispatcher private val io: CoroutineDispatcher
) : CatalogoEquiposRepository {

    // Si Room falla, el código se ofrece como fuera del catálogo: el registro lo vuelve a validar (RF-907)
    override suspend fun existe(codigo: String): Boolean = withContext(io) {
        try {
            catalogoDao.equipoPorCodigo(codigo) != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo consultar el catálogo para $codigo", e)
            false
        }
    }

    private companion object {
        const val TAG = "CatalogoEquiposRepository"
    }
}
