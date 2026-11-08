package com.softcorp.sigtec.core.theme

import androidx.compose.ui.graphics.Color

// Paleta v2 (DESIGN.md, sección 3): teal de marca sobre un fondo teal muy claro, con tarjetas blancas.
// Los roles que DESIGN.md no fija se derivan del mismo tono y tienen 4,5:1 o más con su texto
// (specs/retema-v2/plan.md, sección 7).

// ---------- Modo claro ----------
internal val PrimarioClaro = Color(0xFF00677E)                  // botón principal, elemento seleccionado
internal val SobrePrimarioClaro = Color(0xFFFFFFFF)
internal val ContenedorPrimarioClaro = Color(0xFFB3EBFF)        // pastilla activa del menú inferior
internal val SobreContenedorPrimarioClaro = Color(0xFF001F28)
internal val SecundarioClaro = Color(0xFF4B6269)                // íconos de apoyo
internal val SobreSecundarioClaro = Color(0xFFFFFFFF)
internal val ContenedorSecundarioClaro = Color(0xFFCEE7EF)      // avatares de otros técnicos, chips de componente
internal val SobreContenedorSecundarioClaro = Color(0xFF061F25)
internal val TerciarioClaro = Color(0xFF3F4670)                 // todo lo generado por IA
internal val SobreTerciarioClaro = Color(0xFFFFFFFF)
internal val ContenedorTerciarioClaro = Color(0xFFE9EAF6)
internal val SobreContenedorTerciarioClaro = Color(0xFF1F2447)
internal val ErrorClaro = Color(0xFFBA1A1A)
internal val SobreErrorClaro = Color(0xFFFFFFFF)
internal val ContenedorErrorClaro = Color(0xFFFFDAD6)
internal val SobreContenedorErrorClaro = Color(0xFF8C1212)
internal val FondoClaro = Color(0xFFEEF4F5)                     // fondo de pantalla
internal val SuperficieClaro = Color(0xFFFFFFFF)                // tarjetas
internal val SobreSuperficieClaro = Color(0xFF0F1E22)
internal val VarianteSuperficieClaro = Color(0xFFE3ECEF)        // pistas de barras, pestañas segmentadas
internal val SobreVarianteSuperficieClaro = Color(0xFF4A5B60)
internal val ContornoClaro = Color(0xFF70787C)                  // borde de campos (4,5:1 sobre blanco)
internal val VarianteContornoClaro = Color(0xFFB9C9CD)          // divisores y chips sin seleccionar
internal val SuperficieInversaClaro = Color(0xFF253236)
internal val SobreSuperficieInversaClaro = Color(0xFFECF2F3)
internal val PrimarioInversoClaro = Color(0xFF5BC8E3)
internal val ContenedorMasBajoClaro = Color(0xFFFFFFFF)
internal val ContenedorBajoClaro = Color(0xFFF7FAFB)
internal val ContenedorClaro = Color(0xFFF1F6F7)
internal val ContenedorAltoClaro = Color(0xFFFFFFFF)            // diálogos blancos
internal val ContenedorMasAltoClaro = Color(0xFFE3ECEF)

// ---------- Modo oscuro ----------
// DESIGN.md trae solo una propuesta sin validar; estos valores llegan a 4,5:1 en texto y a 3:1 en
// bordes y puntos (plan del retema, sección 7).
internal val PrimarioOscuro = Color(0xFF5BC8E3)
internal val SobrePrimarioOscuro = Color(0xFF00323E)
internal val ContenedorPrimarioOscuro = Color(0xFF004D5F)
internal val SobreContenedorPrimarioOscuro = Color(0xFFB3EBFF)
internal val SecundarioOscuro = Color(0xFFB2CBD3)
internal val SobreSecundarioOscuro = Color(0xFF1D343A)
internal val ContenedorSecundarioOscuro = Color(0xFF334A51)
internal val SobreContenedorSecundarioOscuro = Color(0xFFCEE7EF)
internal val TerciarioOscuro = Color(0xFFBEC3EE)
internal val SobreTerciarioOscuro = Color(0xFF282E58)
internal val ContenedorTerciarioOscuro = Color(0xFF2E3352)
internal val SobreContenedorTerciarioOscuro = Color(0xFFE0E2F7)
internal val ErrorOscuro = Color(0xFFFFB4AB)
internal val SobreErrorOscuro = Color(0xFF690005)
internal val ContenedorErrorOscuro = Color(0xFF93000A)
internal val SobreContenedorErrorOscuro = Color(0xFFFFDAD6)
internal val FondoOscuro = Color(0xFF0F1A1D)
internal val SuperficieOscuro = Color(0xFF172327)
internal val SobreSuperficieOscuro = Color(0xFFE3ECEF)
internal val VarianteSuperficieOscuro = Color(0xFF2A3A3F)
internal val SobreVarianteSuperficieOscuro = Color(0xFFA9BBC0)
internal val ContornoOscuro = Color(0xFF8A9A9F)
internal val VarianteContornoOscuro = Color(0xFF56686D)         // también es el borde de las tarjetas (3:1 sobre el fondo)
internal val SuperficieInversaOscuro = Color(0xFFE3ECEF)
internal val SobreSuperficieInversaOscuro = Color(0xFF253236)
internal val PrimarioInversoOscuro = Color(0xFF00677E)
internal val ContenedorMasBajoOscuro = Color(0xFF0B1416)
internal val ContenedorBajoOscuro = Color(0xFF172327)
internal val ContenedorOscuro = Color(0xFF1C2A2F)
internal val ContenedorAltoOscuro = Color(0xFF22313A)
internal val ContenedorMasAltoOscuro = Color(0xFF2A3A3F)
