# Spec 001 — Inicio de sesión y permisos por perfil (HU-01)

> Spec escrita después de implementar la HU (PR #5), a partir del código en `main`. RF-1 a RF-12 describen lo que la app hace hoy, salvo los cambios marcados «(pendiente)»; RF-13, RF-14 y RF-15 están **pendientes de implementar**. El cómo está en `plan.md`.

## Contexto y objetivo
SIGTEC guarda incidencias de toda el área de Soporte y Sistemas, y cada perfil puede hacer cosas distintas: el jefe asigna, el técnico atiende, el personal de sistemas consulta. Sin una sesión que identifique a la persona y su perfil, cualquiera que tome el celular vería y cambiaría información que no le corresponde. Esta HU permite entrar con la cuenta institucional y limita lo que cada perfil puede hacer, tanto en el menú como en las reglas del dominio.

## Usuarios / actores
- Jefe de Soporte y Sistemas, técnico y personal de sistemas: cualquiera con cuenta `@softcorp.pe` y un perfil asignado.
- Firebase Authentication (valida la contraseña) y Firestore (`usuarios/{uid}`, guarda el perfil).

## Historias de usuario
- HU-01: Como usuario del área de Soporte y Sistemas quiero iniciar sesión con mi correo institucional y mi contraseña para usar solo las funciones de mi perfil.

## Requisitos funcionales (criterios de aceptación en EARS)
- RF-1: CUANDO el usuario ingresa un correo y una contraseña válidos y pulsa «Ingresar», EL SISTEMA abre la sesión y muestra el inicio con el menú de su perfil.
- RF-2: MIENTRAS el correo o la contraseña estén vacíos, o haya un ingreso en curso, EL SISTEMA mantiene deshabilitado el botón «Ingresar».
- RF-3: EL SISTEMA normaliza el correo (sin espacios y en minúsculas) antes de validarlo.
- RF-4: SI el correo no existe o la contraseña es incorrecta, ENTONCES EL SISTEMA muestra «Correo o contraseña incorrectos.» sin indicar cuál de los dos falló.
- RF-5: SI no hay conexión al autenticarse o al leer el perfil en `usuarios/{uid}`, ENTONCES EL SISTEMA muestra «Sin conexión. Revisa tu red e inténtalo de nuevo.». (pendiente: la falla al leer el perfil hoy muestra «Ocurrió un error inesperado»)
- RF-5b (pendiente): SI algo falla después de autenticarse en Firebase, ENTONCES EL SISTEMA cierra la sesión de Firebase antes de mostrar el error, para no dejar una sesión sin perfil.
- RF-6: SI Firebase bloquea por demasiados intentos, ENTONCES EL SISTEMA muestra «Demasiados intentos. Espera unos minutos.».
- RF-7: SI la cuenta existe pero no tiene perfil válido en `usuarios/{uid}`, ENTONCES EL SISTEMA cierra la sesión de Firebase y muestra «Tu cuenta no tiene un perfil asignado. Consulta al jefe del área.».
- RF-8: EL SISTEMA muestra en el menú solo los destinos que el perfil puede usar (`DestinoPrincipal.para(perfil)`).
- RF-9: CUANDO se ejecuta una acción protegida, EL SISTEMA verifica el permiso en el dominio con `VerificarPermiso` y devuelve `SinPermiso` si no hay sesión o el perfil no lo tiene, aunque el menú haya mostrado la opción.
- RF-10: CUANDO el usuario pulsa «Cerrar sesión» y no hay cambios sin enviar, o confirma el cierre según RF-13, EL SISTEMA borra los datos locales del celular y vuelve a la pantalla de acceso. «Datos locales» son la base Room y, si quien sale es el dueño de la huella, sus preferencias de huella (HU-02). Las fotos se agregarán cuando exista la HU de captura.
- RF-11: CUANDO inicia sesión una persona distinta de la que tenía la sesión anterior, EL SISTEMA borra los datos locales de la anterior antes de mostrar nada. (pendiente) SI la sesión anterior es de un dueño bloqueado con cambios sin enviar, ENTONCES EL SISTEMA muestra antes «{Nombre} tiene N cambios sin enviar en este celular. Si ingresas, se perderán.» con «Cancelar» e «Ingresar de todos modos», y solo continúa si el usuario confirma.
- RF-12: CUANDO se abre la app con una sesión de Firebase vigente y sin huella vinculada, EL SISTEMA entra directo sin pedir la contraseña.
- RF-13 (pendiente): SI el usuario pulsa «Cerrar sesión» y hay cambios sin enviar (`EstadoDatos.pendientesPorEnviar > 0`: incidencias y movimientos sin subir), ENTONCES EL SISTEMA muestra «Tienes N cambios sin enviar. Si cierras sesión ahora, se perderán.» («1 cambio» en singular) con las opciones «Cancelar» y «Cerrar de todos modos», y solo borra los datos locales si el usuario confirma. Cancelar, o tocar fuera del diálogo, deja al usuario en Ajustes de seguridad sin cambios.
- RF-14 (pendiente): CUANDO el usuario pulsa «Cerrar sesión» y no hay cambios sin enviar, EL SISTEMA cierra la sesión sin pedir confirmación.
- RF-15 (pendiente): SI el perfil de `usuarios/{uid}` se borra o deja de ser válido con la sesión abierta, ENTONCES EL SISTEMA cierra la sesión y borra los datos locales sin pedir confirmación, porque la decisión la tomó un administrador y quien ya no tiene perfil no debe conservar datos del área.

## Requisitos no funcionales
- Seguridad: la contraseña solo se envía a Firebase Authentication; la app no la guarda en Room, DataStore ni registros.
- Seguridad: los permisos de `Perfil.kt` coinciden con los de `firebase/firestore.rules`.
- Idioma: textos de UI y mensajes de error en español; cada error sale de `error.mensaje()`.
- Plataforma: Android 8.0 (minSdk 26) o superior.

## Casos límite
- Correo con espacios o mayúsculas: se normaliza (RF-3).
- Correo vacío enviado por código, aunque la UI lo impide: devuelve `CredencialesInvalidas` sin llamar a Firebase.
- Perfil con un valor que no existe en `Perfil` (por ejemplo, un texto mal escrito en Firestore): se trata como sin perfil (RF-7).
- El perfil cambia a otro perfil válido con la sesión abierta: el usuario y su menú se actualizan en vivo, porque se escucha `usuarios/{uid}`. Si deja de ser válido, aplica RF-15.
- Campo con solo espacios: cuenta como vacío (RF-2).
- Espacios dentro del correo: no se quitan; solo se quitan los de los extremos (RF-3).
- Cuenta deshabilitada en Firebase: se muestra «Correo o contraseña incorrectos.», igual que RF-4.
- Doble pulsación de «Ingresar» o rotación de pantalla con un ingreso en curso: el botón está deshabilitado mientras carga (RF-2) y el ViewModel sobrevive a la rotación, así que no se envían dos ingresos.
- Abrir la app sin conexión, con la sesión vigente y sin el perfil en la caché de Firestore: se muestra la pantalla de acceso.
- El dominio `@softcorp.pe` no se valida en la app: las cuentas solo las crea el jefe en la consola de Firebase.
- Error no clasificado: se registra con `Log.w` (TAG `SesionRepository`) y se muestra «Ocurrió un error inesperado. Inténtalo de nuevo.».
- Cerrar sesión con registros sin sincronizar: hoy se pierden sin aviso; RF-13 lo corrige.
- Cerrar sesión con registros sin enviar y sin conexión: aplica igual RF-13; el aviso no depende de la red.

## Fuera de alcance
- Ingreso y desbloqueo con huella, y la opción «Bloquear»: HU-02.
- Crear cuentas, recuperar o cambiar la contraseña: se hace desde la consola de Firebase.
- Asignar o cambiar perfiles desde la app.
- Inicio de sesión con Google u otros proveedores.

## Criterios de finalización
- RF-1 a RF-15 implementados.
- Pruebas según la sección 7 de `plan.md`. Hoy existen `SesionYPermisosTest` (RF-4, RF-9), `EstadoAccesoTest` (RF-12) y `LoginViewModelTest` (RF-2, RF-3, RF-4). RF-5, RF-6, RF-7 y RF-15 se verifican a mano porque dependen de Firebase.
- `./gradlew assembleDebug testDebugUnitTest lintDebug` en verde.
- Prueba manual en celular: entrar con un usuario de cada perfil y comprobar su menú; contraseña incorrecta; modo avión al ingresar; cerrar sesión y entrar con otra cuenta sin ver datos de la anterior; registrar una incidencia en modo avión y comprobar el aviso al cerrar sesión.

## Dudas abiertas
- [NECESITA ACLARACIÓN] Enlazar RF-1 a RF-15 con los RF de la especificación del producto: falta saber qué RF del producto corresponden a la HU-01 (el documento no está en el repo).
