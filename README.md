# Integración CI/CD con Fastlane

Esta guía muestra cómo configurar un pipeline de CI/CD para tu aplicación Flutter usando Fastlane y GitHub Actions.

## Configuración Actual

### Fastlane iOS (`ios/fastlane/Fastfile`)

```ruby
# Lanes disponibles para iOS:
fastlane ios build_dev          # Build desarrollo (ad-hoc)
fastlane ios build_staging      # Build staging (app-store)
fastlane ios build_production   # Build producción (app-store)
fastlane ios deploy_dev         # Deploy a Firebase App Distribution
fastlane ios deploy_staging     # Deploy a TestFlight
fastlane ios deploy_production  # Deploy a App Store
```

### GitHub Actions Workflow

El pipeline incluye dos jobs principales de deployment:

1. **build-production** (Android): Usa Fastlane para deploy a Google Play
2. **build-ios-prod** (iOS): Build nativo con certificados y provisioning profiles

## Variables de Entorno Requeridas

### Para Android (Google Play Store)
```
PLAY_STORE_CONFIG_JSON=<service-account-json-base64>
KEYSTORE_BASE64=<keystore-file-base64>
STORE_PASSWORD=<keystore-password>
KEY_PASSWORD=<key-password>
KEY_ALIAS=<key-alias>
```

### Para iOS (App Store/TestFlight)
```
# Certificados y perfiles
IOS_P12_CERT_BASE64=<certificate-p12-base64>
IOS_P12_CERT_PASSWORD=<certificate-password>
IOS_PROVISION_PROFILE_BASE64=<provisioning-profile-base64>

# App Store Connect API (para Fastlane)
APP_STORE_CONNECT_API_KEY_ID=<api-key-id>
APP_STORE_CONNECT_ISSUER_ID=<issuer-id>
APP_STORE_CONNECT_API_KEY_CONTENT=<api-key-content>

# Firebase App Distribution
FIREBASE_APP_ID_IOS_DEV=<firebase-app-id>
FIREBASE_CLI_TOKEN=<firebase-token>
```

## Cómo Usar

### 1. Deployment Manual con Fastlane

```bash
# Para iOS
cd ios
fastlane deploy_dev         # Firebase App Distribution
fastlane deploy_staging     # TestFlight
fastlane deploy_production  # App Store

# Para Android
cd android
fastlane deploy_production  # Google Play Store
```

### 2. Deployment Automático

El workflow se ejecuta en push a la rama `dev` pero los jobs están deshabilitados (`if: false`).

Para habilitar:
1. Cambia `if: false` a `if: true` en los jobs deseados
2. Configura todos los secrets necesarios en GitHub
3. Push a la rama `dev`

## Estructura de Archivos

```
├── .github/workflows/flutter-ci.yml    # GitHub Actions workflow
├── ios/fastlane/Fastfile               # Configuración Fastlane iOS
├── android/fastlane/Fastfile           # Configuración Fastlane Android
└── ios/ExportOptionsProduction.plist   # Opciones de exportación iOS
```

## Próximos Pasos

1. **Habilitar jobs**: Cambiar `if: false` a `if: true`
2. **Configurar secrets**: Añadir todas las variables en GitHub Settings > Secrets
3. **Certificados iOS**: Configurar certificados de distribución y provisioning profiles
4. **Testing**: Habilitar jobs de análisis y testing (`analyze`, `test`)

## Comandos Útiles

```bash
# Instalar Fastlane
gem install fastlane

# Ver lanes disponibles
fastlane lanes

# Ejecutar con verbose para debug
fastlane ios deploy_dev --verbose
```
