# PrimePlex 🎬

Aplicación de streaming y catálogo cinematográfico estilo **Plex TV** con interfaz cinematográfica moderna inspirada en **Amazon Prime Video**.

---

## 🍏 Instalador Oficial para macOS (.DMG)

Puedes instalar PrimePlex directamente en tu Mac (compatible con **Apple Silicon M1, M2, M3, M4** e **Intel**):

1. **Descarga directa desde este repositorio de GitHub:**
   * 👉 Haz clic en [**`PrimePlex-macOS.dmg`**](./PrimePlex-macOS.dmg) en la lista de archivos de este repositorio y pulsa **Download** (o en la carpeta [`macos/PrimePlex-macOS.dmg`](./macos/PrimePlex-macOS.dmg)).
2. **Instalación en Mac:**
   * Haz doble clic sobre el archivo `PrimePlex-macOS.dmg` descargado.
   * Arrastra el icono **PrimePlex** hacia la carpeta **Applications** (Aplicaciones).
   * ¡Listo! Ya tienes la app instalada en tu Mac sin necesidad de ningún emulador.

---

## 📱 Cómo descargar e instalar el archivo APK para Android desde GitHub

Tienes **3 formas sencillas** de obtener el archivo `.apk`:

### 1. Desde las "Releases" de GitHub (Recomendado)
1. En la página principal de este repositorio de GitHub, dirígete a la sección lateral derecha llamada **Releases** (o entra en `/releases`).
2. En la versión **`PrimePlex - Última Versión APK`**, haz clic en el archivo adjunto:
   👉 **`PrimePlex-debug.apk`**
3. El archivo se descargará directamente a tu teléfono o ordenador.
4. En tu dispositivo Android, abre el archivo descargado para instalarlo (activa "Permitir desde esta fuente" si el sistema te lo solicita).

---

### 2. Desde "Actions" de GitHub (Artefactos automáticos)
1. En la barra superior de este repositorio en GitHub, haz clic en la pestaña **Actions**.
2. Selecciona la ejecución más reciente del flujo de trabajo **`Generar APK de PrimePlex`**.
3. En la parte inferior de la página (sección **Artifacts**), haz clic en **`PrimePlex-APK`**.
4. Se descargará un archivo ZIP que contiene el APK listo para instalar.

---

### 3. Descarga directa desde Google AI Studio
Si tienes el proyecto abierto en el entorno de Google AI Studio:
1. En la esquina superior derecha, abre el menú de opciones (**Settings / Export**).
2. Selecciona **Download APK** o **Push to GitHub** para sincronizar tu repositorio automáticamente.

---

## 🌐 Dominio Gratuito Oficial en GitHub Pages

Tu proyecto tiene asignado un **dominio gratuito oficial de GitHub** con certificado SSL (HTTPS) y CDN global de alta velocidad:

* 🚀 **Tu Dominio Gratuito de GitHub:**
  👉 [**`https://djyuanyo.github.io/Mencia-Media/`**](https://djyuanyo.github.io/Mencia-Media/)

### 🌟 ¿Qué funciones tiene tu dominio de GitHub Pages?
1. **Portal Web Oficial PrimePlex:**
   * Página web moderna con la estética cinematográfica de **Amazon Prime Video**.
   * Muestra las características de la aplicación, carrusel de películas y enlaces de acceso.
2. **Descarga Directa del APK:**
   * Los usuarios pueden descargar directamente el archivo `PrimePlex-debug.apk` desde tu web.
3. **Android App Links & Deep Linking:**
   * La app está vinculada a `djyuanyo.github.io`. Los enlaces tipo `https://djyuanyo.github.io/Mencia-Media/watch/{id}` abren directamente la película dentro de la app instalada.
4. **Despliegue 100% Automático:**
   * El flujo de GitHub Actions (`.github/workflows/build-apk.yml`) compila el APK y publica la web automáticamente en GitHub Pages en cada push a `main`.

### ⚙️ Activación rápida en tu repositorio de GitHub (si aún no está activo):
1. Ve a tu repositorio en GitHub: `https://github.com/djyuanyo/Mencia-Media`
2. Entra en **Settings** (pestaña superior derecha).
3. En el menú izquierdo, haz clic en **Pages**.
4. En **Build and deployment** > **Source**, selecciona **GitHub Actions**.
5. ¡Listo! Cada vez que el flujo termine de compilar, tu web estará publicada en `https://djyuanyo.github.io/Mencia-Media/`.

---

## 🌐 Dominios Adicionales de Firebase Asociados

Tu app está vinculada a los dominios gratuitos oficiales de **Firebase Hosting**:

* 🔗 **Dominio Principal:** [`https://gen-lang-client-0763337447.web.app`](https://gen-lang-client-0763337447.web.app)
* 🔗 **Dominio Alternativo:** [`https://gen-lang-client-0763337447.firebaseapp.com`](https://gen-lang-client-0763337447.firebaseapp.com)

### ¿Qué incluye esta asociación?
1. **Android App Links (Digital Asset Links):**
   * Configurado en `public/.well-known/assetlinks.json` y `AndroidManifest.xml` con verificación automática (`autoVerify="true"`).
   * Al abrir un enlace del dominio en Android, se abre directamente dentro de la aplicación PrimePlex sin pasar por el navegador.
2. **Portal Web & Descarga:**
   * La carpeta `public/` contiene una web estilo Amazon Prime Video para promocionar la app, mostrar los enlaces de descarga directa del APK e interactuar con la app.
3. **Cómo desplegar en Firebase Hosting:**
   * **Desde la terminal local con Firebase CLI:**
     ```bash
     firebase login
     firebase deploy --only hosting
     ```
   * **Automático en GitHub Actions:** Puedes añadir el secreto `FIREBASE_TOKEN` en GitHub Secrets y se desplegará automáticamente con cada compilación.

---

## 🛠️ Características Principales
* 🌐 **Integración de Metadatos Multi-Fuente**:
  * **TMDB (The Movie Database)**: Resúmenes en español, imágenes en alta resolución y reparto oficial.
  * **IMDb**: Puntuaciones oficiales (con insignias doradas) y enlaces a fichas oficiales.
  * **TheTVDB**: Orden oficial de temporadas y episodios para series de televisión.
* 📂 **Catálogo Flexible**: Permite guardar contenidos con o sin enlace de vídeo; los títulos sin enlace aparecen en tu biblioteca con todos sus metadatos y fichas técnicas.
* 👨‍👩‍👧‍👦 **Gestión de Perfiles**: Múltiples perfiles con soporte para perfil infantil protegido.
* ⏱️ **Progreso de Reproducción**: Memoria de reanudación y barra de progreso sincronizada localmente con Room Database.
