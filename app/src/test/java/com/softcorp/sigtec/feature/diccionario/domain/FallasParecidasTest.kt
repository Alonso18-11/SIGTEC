package com.softcorp.sigtec.feature.diccionario.domain

import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.falla
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.fuente
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.impresora
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.office
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.pantallaAzul
import com.softcorp.sigtec.feature.diccionario.domain.FallasDePrueba.todas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-13 (RF-1325, RF-1326): el parecido se calcula en el celular, por palabras con peso. */
class FallasParecidasTest {

    @Test
    fun `la descripcion de una impresora sugiere primero la falla de la impresora`() {
        val r = fallasParecidas("La impresora del área de contabilidad no imprime, parece que no está en la red", todas)
        assertEquals(impresora, r.first())
    }

    @Test
    fun `tildes mayusculas y signos no cambian el resultado`() {
        val con = fallasParecidas("¡La PC NO ENCIENDE después del corte de luz!", todas)
        val sin = fallasParecidas("la pc no enciende despues del corte de luz", todas)
        assertEquals(fuente, con.first())
        assertEquals(con, sin)
    }

    @Test
    fun `singular y plural coinciden`() {
        assertEquals(raiz("luz"), raiz("luces"))
        assertEquals(raiz("fuente"), raiz("fuentes"))
        assertEquals(raiz("error"), raiz("errores"))
        assertEquals(raiz("red"), raiz("redes"))
        assertEquals(raiz("impresora"), raiz("impresoras"))
    }

    @Test
    fun `las palabras vacias y las cortas no cuentan`() {
        assertEquals(emptySet<String>(), palabrasClave("el de la que con para una no en"))
        assertTrue(fallasParecidas("no se puede, es que de la", todas).isEmpty())
    }

    @Test
    fun `sin palabras en comun no sugiere nada`() {
        assertTrue(fallasParecidas("El mouse hace doble clic solo", todas).isEmpty())
        assertTrue(fallasParecidas("", todas).isEmpty())
        assertTrue(fallasParecidas("La impresora no imprime", emptyList()).isEmpty())
    }

    @Test
    fun `devuelve como maximo tres`() {
        val muchas = (1..6).map { falla("x$it", "Impresora $it atascada", "Impresora", "Papel atascado") }
        assertEquals(3, fallasParecidas("La impresora tiene papel atascado", muchas).size)
    }

    @Test
    fun `una palabra en el titulo pesa mas que en los sintomas`() {
        val enTitulo = falla("a", "Teclado sin respuesta", "Periférico", "No escribe")
        val enSintomas = falla("b", "Puerto USB dañado", "Placa", "El teclado no responde en ese puerto")
        assertEquals(listOf(enTitulo, enSintomas), fallasParecidas("El teclado dejó de funcionar", listOf(enSintomas, enTitulo)))
    }

    @Test
    fun `una palabra que esta en todas las fallas casi no cuenta frente a una rara`() {
        val comun = "Windows"
        val a = falla("a", "Windows lento al iniciar", "Sistema operativo", "Tarda mucho")
        val b = falla("b", "Windows no actualiza", "Sistema operativo", "Error de actualización")
        val c = falla("c", "Windows sin sonido", "Audio", "No suena el parlante")
        val r = fallasParecidas("$comun no tiene sonido en el parlante", listOf(a, b, c))
        assertEquals(c, r.first())
    }

    @Test
    fun `con el mismo puntaje va primero el titulo alfabeticamente menor`() {
        val b = falla("b", "Bandeja rota", "Impresora", "Sin papel")
        val a = falla("a", "Atasco de papel", "Impresora", "Sin papel")
        assertEquals(listOf(a, b), fallasParecidas("La impresora", listOf(b, a)))
    }

    @Test
    fun `office y la pantalla azul se distinguen`() {
        assertEquals(office, fallasParecidas("Excel dice que el producto no tiene licencia", todas).first())
        assertEquals(pantallaAzul, fallasParecidas("Sale pantalla azul y se reinicia", todas).first())
    }

    @Test
    fun `con 500 fallas responde en menos de 3 segundos`() {
        // RF-1306: el diccionario completo del celular
        val grandes = (1..500).map {
            falla(
                "g$it", "Falla número $it del equipo de prueba con texto largo", "Componente $it",
                "Síntomas variados de la falla $it: no enciende, hace ruido, se reinicia, la pantalla parpadea " +
                    "y el ventilador suena fuerte cada vez que se abre un programa pesado en el área $it"
            )
        }
        val inicio = System.nanoTime()
        fallasParecidas("La pantalla parpadea y el ventilador hace ruido al abrir programas", grandes)
        val segundos = (System.nanoTime() - inicio) / 1e9
        assertTrue("Tardó $segundos s", segundos < 3)
    }
}
