# Publicar SIGTEC en Google Play

Guía para el requisito 12 del curso (sección 19 del documento del producto). Lo que exige una cuenta, un pago o una
contraseña lo hace la persona que publica: nunca va en el repo ni lo hace un asistente.

## 0. Antes de empezar

- **Costo.** Google Play cobra un pago único de 25 USD por la cuenta de desarrollador. Sin esa cuenta no hay ningún
  canal de Play, ni siquiera la prueba interna. Es el único costo del proyecto.
- **Cuenta personal nueva.** Antes de pedir acceso a producción, debe pasar una prueba cerrada con al menos 12 testers
  inscritos durante 14 días seguidos (regla vigente en noviembre de 2026; verificarla en Play Console antes de
  empezar). Si un tester sale, el conteo puede reiniciarse: conviene invitar a unos 15.
- **Alternativa sin costo (no es Google Play).** Firebase App Distribution, incluido en el plan Spark: se invita a
  testers por correo y reciben la app firmada. Sirve solo si la docente la acepta como evidencia.
- **Versión de Android.** Desde el 31 de agosto de 2026, las apps nuevas deben apuntar a Android 16 (API 36) o más.
  SIGTEC apunta a la API 37.

## 1. Clave de subida (una sola vez)

Google firma la app que llega a los celulares (Play App Signing); nosotros solo firmamos lo que subimos, con la
**clave de subida**. Si se pierde, se pide otra a Google; aun así, guárdala con su contraseña en un lugar seguro
(gestor de contraseñas) y fuera del repo.

1. Desde la carpeta del proyecto, genera la clave **fuera** del repo (Android Studio trae `keytool` en su JBR):
   ```bash
   "C:/Program Files/Android/Android Studio/jbr/bin/keytool" -genkeypair -v -keystore ../sigtec-subida.jks -alias sigtec -keyalg RSA -keysize 2048 -validity 10000
   ```
   Te pide una contraseña (la eliges tú) y tus datos (nombre, organización, país).
2. Copia `keystore.properties.example` como `keystore.properties` en la raíz del proyecto y pon tu contraseña.
   `keystore.properties` y los `.jks` están en `.gitignore`. La ruta de `storeFile` es relativa a la raíz del
   proyecto y va con `/`: en ese archivo la barra invertida es un carácter de escape, así que una ruta de Windows
   como `C:\Users\...` no funciona.
3. Compila el paquete firmado:
   ```bash
   ./gradlew bundleRelease
   ```
   Queda en `app/build/outputs/bundle/release/app-release.aab`. Sin `keystore.properties`, el release sale sin
   firma, que es lo que hace el CI.
4. **Cada subida nueva** necesita un `versionCode` mayor en `app/build.gradle.kts` (1, 2, 3…) y, si cambia algo
   visible, un `versionName` nuevo (1.0, 1.1…). Va en su propio PR, porque es un archivo compartido.

## 2. Play Console

1. Crear la cuenta en <https://play.google.com/console> (personal), pagar los 25 USD y verificar la identidad.
2. **Crear app:** nombre «SIGTEC», idioma español (Latinoamérica), tipo App, gratuita.
3. **Configurar la app** (panel «Configura tu app»):
   - Política de privacidad: `https://alonso18-11.github.io/SIGTEC/privacidad.html` (sección 4).
   - Acceso a la app: «Todas o algunas funciones están restringidas». Dar a Google una cuenta de prueba **técnico**
     y otra **jefe**, con sus contraseñas, escritas solo en Play Console. Explicar que la huella es opcional.
   - Anuncios: no tiene.
   - Clasificación de contenido: cuestionario IARC, categoría «Utilidades / productividad»; sin violencia, sin
     interacción entre usuarios desconocidos, sin compras.
   - Público objetivo: mayores de 18 años (personal de una empresa).
   - Seguridad de los datos: sección 3.
   - Apps gubernamentales, financieras o de salud: no aplica.
4. **Ficha de Play Store:** descripción breve y completa, ícono de 512 × 512, gráfico de funciones de 1024 × 500 y
   al menos 2 capturas de teléfono (en claro y oscuro). Las capturas se toman con `adb exec-out screencap -p`.
5. **Prueba cerrada:** Pruebas › Prueba cerrada › crear pista, subir el `.aab`, agregar la lista de testers (correos de
   Gmail) y enviar a revisión. Cada tester acepta la invitación desde el enlace de inscripción y debe **quedarse**
   14 días.
6. Pasados los 14 días con 12 o más inscritos: Panel › «Solicitar acceso a producción», responder el cuestionario y,
   con el visto bueno, publicar en producción. Si la revisión no llega a tiempo para la entrega, la prueba cerrada ya
   evidencia el despliegue (sección 19.2 del producto).

## 3. Respuestas para «Seguridad de los datos»

Lo que **sale del celular** y por qué. Todo viaja cifrado (HTTPS de Firebase). No se vende ni se comparte con
terceros con fines publicitarios.

| Tipo de dato (Play) | ¿Se recoge? | Para qué | Notas |
|---|---|---|---|
| Información personal › Correo electrónico | Sí, obligatorio | Funcionalidad de la app, gestión de la cuenta | Inicio de sesión con Firebase Authentication |
| Información personal › Nombre | Sí, obligatorio | Funcionalidad de la app | Perfil del usuario y nombre de quien registra o atiende |
| Información personal › ID de usuario | Sí, obligatorio | Funcionalidad de la app, gestión de la cuenta | UID de Firebase |
| Información personal › Otra información | Sí, obligatorio | Funcionalidad de la app | Nombre del responsable del equipo escrito en la incidencia |
| Actividad en apps › Otro contenido generado por el usuario | Sí, obligatorio | Funcionalidad de la app | Incidencias, informes técnicos y movimientos en Firestore |
| Fotos y videos | **No** | — | Las fotos de evidencia quedan solo en el celular, fuera de la copia de seguridad (HU-09) |
| Audio › Grabaciones de voz | **No** | — | El dictado lo hace el reconocimiento de voz del celular; la app no recibe ni guarda el audio (HU-10) |
| Datos biométricos (huella) | **No** | — | Android solo dice si la verificación fue correcta (HU-02) |
| Ubicación, contactos, finanzas, salud, mensajes | No | — | — |

- ¿Los usuarios pueden pedir que se borren sus datos? Las cuentas las crea el área (no se registran desde la app):
  la baja se pide al jefe del área, que borra la cuenta en Firebase. Cerrar sesión borra todos los datos del celular.
- Cuando se agregue la IA (HU-14, Firebase AI Logic) o las API de Logística y del directorio (HU-11, HU-12), esta
  tabla se revisa en el mismo PR.

## 4. Política de privacidad

El texto está en `docs/privacidad.html`. Para publicarla: GitHub › Settings › Pages › «Deploy from a branch» ›
rama `main`, carpeta `/docs`. Queda en `https://alonso18-11.github.io/SIGTEC/privacidad.html` y se actualiza con cada
PR que la cambie.
