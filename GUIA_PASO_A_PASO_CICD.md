# Guía Paso a Paso: Configuración CI/CD con Fastlane

Esta guía te llevará paso a paso para configurar completamente el CI/CD con Fastlane para tu app Flutter.

## 📋 Tabla de Contenidos

1. [Prerequisitos](#prerequisitos)
2. [Configuración de Android](#configuración-de-android)
3. [Configuración de iOS](#configuración-de-ios)
4. [Configuración de GitHub Actions](#configuración-de-github-actions)
5. [Testing y Deployment](#testing-y-deployment)
6. [Solución de Problemas](#solución-de-problemas)

---

## Prerequisitos

### ✅ 1. Instalar herramientas necesarias

```bash
# Instalar Fastlane
gem install fastlane

# Verificar instalación
fastlane --version
```

### ✅ 2. Configurar cuentas necesarias

- **Google Play Console**: Cuenta de desarrollador activa
- **Apple Developer Program**: Cuenta de desarrollador activa  
- **Firebase**: Proyecto configurado (para distribución de desarrollo)
- **GitHub**: Repositorio con permisos de administrador

---

## Configuración de Android

### 🤖 Paso 1: Generar keystore de firma

```bash
# Crear keystore para producción
keytool -genkey -v -keystore release-key.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000

# Guardar en android/
mv release-key.keystore android/
```

### 🤖 Paso 2: Crear service account en Google Play Console

1. Ve a Google Play Console → Configuración → Acceso a la API
2. Crea un nuevo proyecto de Google Cloud si no tienes uno
3. Crear cuenta de servicio:
   - Nombre: `fastlane-deployer`
   - Rol: `Editor de servicio`
4. Generar clave JSON y descargarla
5. En Play Console, invita la cuenta de servicio con permisos de `Release manager`

### 🤖 Paso 3: Configurar Fastlane Android

```bash
cd android
fastlane init
```

Crear `android/fastlane/Fastfile`:

```ruby
default_platform(:android)

platform :android do
  desc "Deploy to Google Play Store"
  lane :deploy_production do
    # Configurar keystore
    keystore_path = "../release-key.keystore"
    
    gradle(
      task: "clean bundleProductionRelease",
      properties: {
        "android.injected.signing.store.file" => keystore_path,
        "android.injected.signing.store.password" => ENV["STORE_PASSWORD"],
        "android.injected.signing.key.alias" => ENV["KEY_ALIAS"],
        "android.injected.signing.key.password" => ENV["KEY_PASSWORD"],
      }
    )
    
    upload_to_play_store(
      json_key: "fastlane/playstore-credential.json",
      aab: "../build/app/outputs/bundle/productionRelease/app-production-release.aab",
      track: "internal",
      skip_upload_apk: true
    )
  end
end
```

### 🤖 Paso 4: Test local Android

```bash
# Configurar variables locales
export STORE_PASSWORD="tu_store_password"
export KEY_PASSWORD="tu_key_password" 
export KEY_ALIAS="release"

# Copiar service account JSON
cp ~/Downloads/service-account.json android/fastlane/playstore-credential.json

# Probar deployment
cd android
fastlane deploy_production
```

---

## Configuración de iOS

### 🍎 Paso 1: Configurar Apple Developer

1. **Crear App ID**:
   - Ve a Apple Developer → Certificates, Identifiers & Profiles
   - Crear nuevo App ID para tu bundle identifier

2. **Crear Provisioning Profile**:
   - Tipo: `App Store Distribution`
   - Selecciona tu App ID
   - Selecciona certificado de distribución
   - Descarga el archivo `.mobileprovision`

3. **Exportar certificado**:
   - Abre Keychain Access
   - Encuentra tu certificado de distribución
   - Exportar como `.p12` con contraseña

### 🍎 Paso 2: Configurar App Store Connect API

1. Ve a App Store Connect → Users and Access → Keys
2. Crear nueva clave:
   - Nombre: `Fastlane CI/CD`
   - Acceso: `Developer`
3. Descargar archivo `.p8`
4. Guardar:
   - Key ID
   - Issuer ID
   - Contenido del archivo .p8

### 🍎 Paso 3: Configurar Firebase App Distribution

1. Ve a Firebase Console → Project Settings → Service Accounts
2. Generar nueva clave privada
3. Instalar Firebase CLI:
   ```bash
   npm install -g firebase-tools
   firebase login:ci
   ```
4. Guardar el token generado

### 🍎 Paso 4: Test local iOS

```bash
# Configurar variables
export APP_STORE_CONNECT_API_KEY_ID="tu_key_id"
export APP_STORE_CONNECT_ISSUER_ID="tu_issuer_id"
export APP_STORE_CONNECT_API_KEY_CONTENT="-----BEGIN PRIVATE KEY-----
tu_clave_privada_aqui
-----END PRIVATE KEY-----"

export FIREBASE_CLI_TOKEN="tu_firebase_token"
export FIREBASE_APP_ID_IOS_DEV="tu_app_id"

# Probar deployment
cd ios
fastlane deploy_dev
```

---

## Configuración de GitHub Actions

### 🔧 Paso 1: Configurar GitHub Secrets

Ve a tu repositorio → Settings → Secrets and variables → Actions

**Para Android:**
```
KEYSTORE_BASE64=<keystore en base64>
STORE_PASSWORD=<contraseña del keystore>
KEY_PASSWORD=<contraseña de la clave>
KEY_ALIAS=release
PLAY_STORE_CONFIG_JSON=<service-account.json en base64>
```

**Para iOS:**
```
IOS_P12_CERT_BASE64=<certificado.p12 en base64>
IOS_P12_CERT_PASSWORD=<contraseña del certificado>
IOS_PROVISION_PROFILE_BASE64=<profile.mobileprovision en base64>
APP_STORE_CONNECT_API_KEY_ID=<key id>
APP_STORE_CONNECT_ISSUER_ID=<issuer id>
APP_STORE_CONNECT_API_KEY_CONTENT=<contenido .p8>
FIREBASE_CLI_TOKEN=<firebase token>
FIREBASE_APP_ID_IOS_DEV=<firebase app id>
```

### 🔧 Paso 2: Generar archivos en base64

```bash
# Para keystore Android
base64 -i android/release-key.keystore | pbcopy

# Para certificado iOS  
base64 -i certificado.p12 | pbcopy

# Para provisioning profile iOS
base64 -i profile.mobileprovision | pbcopy

# Para service account JSON
base64 -i service-account.json | pbcopy
```

### 🔧 Paso 3: Habilitar workflows

Edita `.github/workflows/flutter-ci.yml`:

```yaml
# Cambiar de:
if: false

# A:
if: true
```

Para ambos jobs: `build-production` y `build-ios-prod`

---

## Testing y Deployment

### 🚀 Paso 1: Primera prueba

1. Haz un cambio pequeño en tu código
2. Commit y push a la rama `dev`:
   ```bash
   git add .
   git commit -m "test: primera prueba de CI/CD"
   git push origin dev
   ```

3. Ve a GitHub → Actions y monitorea la ejecución

### 🚀 Paso 2: Verificar deployment

**Android:**
- Ve a Google Play Console → Testing → Internal testing
- Verifica que el APK/AAB se subió correctamente

**iOS:**
- Para desarrollo: Ve a Firebase Console → App Distribution
- Para staging: Ve a App Store Connect → TestFlight

### 🚀 Paso 3: Deployment manual de emergencia

Si GitHub Actions falla, puedes deployar manualmente:

```bash
# Android
cd android
fastlane deploy_production

# iOS
cd ios
fastlane deploy_dev      # Para desarrollo
fastlane deploy_staging  # Para TestFlight
```

---

## Solución de Problemas

### ❌ Error: "No matching provisioning profiles found"

**Solución:**
```bash
# Verificar que el profile está bien instalado
security cms -D -i ~/Library/MobileDevice/Provisioning\ Profiles/profile.mobileprovision

# Verificar bundle ID en el profile
grep -A 1 application-identifier ~/Library/MobileDevice/Provisioning\ Profiles/profile.mobileprovision
```

### ❌ Error: "Keystore was tampered with, or password was incorrect"

**Solución:**
```bash
# Verificar contraseña del keystore
keytool -list -v -keystore android/release-key.keystore

# Regenerar keystore si es necesario
keytool -genkey -v -keystore new-release-key.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```

### ❌ Error: "Invalid App Store Connect API key"

**Solución:**
1. Verificar que la clave no haya expirado en App Store Connect
2. Verificar formato del contenido de la clave:
   ```
   -----BEGIN PRIVATE KEY-----
   [contenido de la clave]
   -----END PRIVATE KEY-----
   ```

### ❌ Error: "Build failed with exit code 65"

**Solución iOS:**
```bash
# Limpiar build cache
cd ios
rm -rf build/
flutter clean
flutter pub get
pod install
```

### ❌ Error de permisos en GitHub Actions

**Solución:**
1. Verificar que todos los secrets están configurados
2. Verificar que el repositorio tiene permisos de Actions habilitados
3. Verificar que la rama `dev` existe y es la correcta

---

## 📞 Contacto y Soporte

Si tienes problemas:

1. **Logs detallados**: Ejecuta con `--verbose`
   ```bash
   fastlane ios deploy_dev --verbose
   ```

2. **GitHub Actions logs**: Ve a la pestaña Actions en tu repositorio

3. **Verificar configuración**:
   ```bash
   # Verificar Fastlane
   fastlane lanes
   
   # Verificar Flutter
   flutter doctor
   
   # Verificar certificados iOS
   security find-identity -p codesigning -v
   ```

---

## ✅ Checklist Final

- [ ] Fastlane instalado y configurado
- [ ] Keystore Android generado y configurado
- [ ] Service Account Google Play creado
- [ ] Certificados iOS exportados
- [ ] Provisioning profiles descargados
- [ ] App Store Connect API configurada
- [ ] Firebase App Distribution configurado
- [ ] Todos los GitHub Secrets configurados
- [ ] Workflows habilitados (`if: true`)
- [ ] Primera prueba exitosa
- [ ] Deployment manual funciona
- [ ] Deployment automático funciona

¡Listo! Tu pipeline de CI/CD debería estar funcionando completamente.