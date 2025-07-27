# Integración de Fastlane con CI/CD - Flutter Multi-Flavor

## 📋 Análisis del Estado Actual

### Estructura del Proyecto
- **3 Flavors identificados**: development, staging, production
- **2 Apps distintas**: staging y production (development para testing local)
- **CI/CD actual**: GitHub Actions con workflows separados
- **Fastlane**: Ya configurado básicamente para Android y iOS

### Configuración Actual
- **Android Fastlane**: Configurado para staging y production
- **iOS Fastlane**: Configuración básica sin lanes específicos
- **CI/CD**: Builds manuales con scripts bash en GitHub Actions

## 🎯 Objetivos de la Integración

1. **Usar Fastlane únicamente para deployment** (mantener tests en GitHub Actions)
2. **Unificar** el proceso de deployment para Android e iOS
3. **Mantener** la separación entre staging y production como apps distintas
4. **Integrar** con los stores (Google Play y App Store)
5. **Conservar** el workflow actual de testing y preparación

## 📝 Plan de Implementación

### Fase 1: Mejora de Fastlane Android

#### 1.1 Actualizar Android Fastfile (Solo Deployment)
```ruby
# android/fastlane/Fastfile
default_platform(:android)

platform :android do
  desc "Deploy staging App Bundle to Play Store Internal"
  lane :deploy_staging do
    # Setup keystore
    setup_staging_keystore
    
    # Build App Bundle
    Dir.chdir("../") do
      sh("flutter", "build", "appbundle", "--flavor", "staging", "-t", "lib/main_staging.dart", "--release")
    end

    # Upload to Play Store
    upload_to_play_store(
      track: 'internal',
      json_key: ENV['GOOGLE_PLAY_JSON_KEY_PATH'],
      package_name: 'com.tuempresa.tuapp.staging',
      aab: '../build/app/outputs/bundle/stagingRelease/app-staging-release.aab'
    )
  end

  desc "Deploy production App Bundle to Play Store"
  lane :deploy_production do
    # Setup keystore
    setup_production_keystore
    
    # Build App Bundle
    Dir.chdir("../") do
      sh("flutter", "build", "appbundle", "--flavor", "production", "-t", "lib/main_production.dart", "--release")
    end

    # Upload to Play Store
    upload_to_play_store(
      track: 'production',
      json_key: ENV['GOOGLE_PLAY_JSON_KEY_PATH'],
      package_name: 'com.tuempresa.tuapp',
      aab: '../build/app/outputs/bundle/productionRelease/app-production-release.aab'
    )
  end

  # Lanes auxiliares para setup de keystores
  private_lane :setup_staging_keystore do
    sh("mkdir", "-p", "../app")
    sh("echo", ENV['KEYSTORE_BASE64'], "|", "base64", "-d", ">", "../app/staging-keystore.jks")
    
    File.open("../key.staging.properties", "w") do |file|
      file.puts "storePassword=#{ENV['STORE_PASSWORD']}"
      file.puts "keyPassword=#{ENV['KEY_PASSWORD']}"
      file.puts "keyAlias=#{ENV['KEY_ALIAS']}"
      file.puts "storeFile=staging-keystore.jks"
    end
  end

  private_lane :setup_production_keystore do
    sh("mkdir", "-p", "../app")
    sh("echo", ENV['KEYSTORE_BASE64'], "|", "base64", "-d", ">", "../app/production-keystore.jks")
    
    File.open("../key.production.properties", "w") do |file|
      file.puts "storePassword=#{ENV['STORE_PASSWORD']}"
      file.puts "keyPassword=#{ENV['KEY_PASSWORD']}"
      file.puts "keyAlias=#{ENV['KEY_ALIAS']}"
      file.puts "storeFile=production-keystore.jks"
    end
  end
end
```

#### 1.2 Configurar Secrets Adicionales
```bash
# Nuevos secrets requeridos en GitHub:
GOOGLE_PLAY_JSON_KEY_PATH  # Ruta al archivo JSON de Google Play
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON  # Contenido del archivo JSON (base64)
```

### Fase 2: Configuración iOS Fastlane

#### 2.1 Actualizar iOS Fastfile (Solo Deployment)
```ruby
# ios/fastlane/Fastfile
default_platform(:ios)

platform :ios do
  desc "Deploy staging to TestFlight"
  lane :deploy_staging do
    setup_ci if ENV['CI']
    
    # Build IPA
    Dir.chdir("../") do
      sh("flutter", "build", "ipa", 
         "--flavor", "staging", 
         "-t", "lib/main_staging.dart", 
         "--release",
         "--export-options-plist=ios/ExportOptionsStaging.plist")
    end

    # Upload to TestFlight
    upload_to_testflight(
      api_key_path: ENV['APP_STORE_CONNECT_API_KEY_PATH'],
      ipa: "build/ios/ipa/staging.ipa",
      skip_waiting_for_build_processing: true
    )
  end

  desc "Deploy production to App Store"
  lane :deploy_production do
    setup_ci if ENV['CI']
    
    # Build IPA
    Dir.chdir("../") do
      sh("flutter", "build", "ipa", 
         "--flavor", "production", 
         "-t", "lib/main_production.dart", 
         "--release",
         "--export-options-plist=ios/ExportOptionsProduction.plist")
    end

    # Upload to App Store
    upload_to_app_store(
      api_key_path: ENV['APP_STORE_CONNECT_API_KEY_PATH'],
      ipa: "build/ios/ipa/production.ipa",
      force: true,
      skip_metadata: false,
      skip_screenshots: true,
      submit_for_review: false
    )
  end
end
```

#### 2.2 Crear ExportOptions.plist para cada flavor
```xml
<!-- ios/ExportOptionsStaging.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store</string>
    <key>teamID</key>
    <string>YOUR_TEAM_ID</string>
    <key>uploadBitcode</key>
    <false/>
    <key>uploadSymbols</key>
    <true/>
    <key>compileBitcode</key>
    <false/>
</dict>
</plist>
```

### Fase 3: Actualizar GitHub Actions

#### 3.1 Modificar Workflow Principal
```yaml
# .github/workflows/flutter-ci.yml
name: Flutter CI with Fastlane

on:
  push:
    branches: [dev, main]
  pull_request:
    branches: [main]

env:
  FLUTTER_CHANNEL: stable
  FLUTTER_VERSION: "3.29.3"
  JAVA_VERSION: "17"
  JAVA_DISTRIBUTION: "temurin"

jobs:
  prepare:
    # ... mantener configuración actual de prepare ...

  # Nuevo job para builds de desarrollo
  build_development:
    name: Build Development (Android)
    needs: prepare
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/dev'
    
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        
      - name: Setup Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.0'
          bundler-cache: true
          working-directory: android
          
      - name: Setup Flutter & Dependencies
        # ... pasos de setup existentes ...
        
      - name: Build Development APK with Fastlane
        working-directory: android
        run: bundle exec fastlane build_development

  # Actualizar build staging
  build_staging:
    name: Deploy Staging with Fastlane
    needs: [prepare, test]
    runs-on: ubuntu-latest
    environment: staging
    if: github.ref == 'refs/heads/dev' && github.event_name == 'push'
    
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        
      - name: Setup Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.0'
          bundler-cache: true
          working-directory: android
          
      - name: Setup Flutter & Dependencies
        # ... pasos existentes ...
        
      - name: Setup Google Play Service Account
        run: |
          echo "${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON }}" | base64 -d > android/google-play-key.json
          
      - name: Deploy Staging with Fastlane
        working-directory: android
        env:
          GOOGLE_PLAY_JSON_KEY_PATH: google-play-key.json
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        run: bundle exec fastlane deploy_staging

  # Actualizar build production
  build_production:
    name: Deploy Production with Fastlane
    needs: [prepare, test]
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    
    steps:
      # ... pasos similares a staging pero con deploy_production ...

  # Nuevo job para iOS
  build_ios_staging:
    name: Deploy iOS Staging with Fastlane
    needs: [prepare, test]
    runs-on: macos-latest
    environment: staging
    if: github.ref == 'refs/heads/dev' && github.event_name == 'push'
    
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4
        
      - name: Setup Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.0'
          bundler-cache: true
          working-directory: ios
          
      - name: Setup Flutter & Dependencies
        # ... pasos de setup ...
        
      - name: Import Code Signing Certificates
        uses: apple-actions/import-codesign-certs@v3
        with:
          p12-file-base64: ${{ secrets.IOS_P12_CERT_BASE64 }}
          p12-password: ${{ secrets.IOS_P12_CERT_PASSWORD }}
          
      - name: Install Provisioning Profile
        run: |
          mkdir -p "$HOME/Library/MobileDevice/Provisioning Profiles"
          echo "${{ secrets.IOS_PROVISION_PROFILE_BASE64 }}" | base64 --decode > "$HOME/Library/MobileDevice/Provisioning Profiles/staging.mobileprovision"
          
      - name: Deploy iOS Staging with Fastlane
        working-directory: ios
        env:
          APP_STORE_CONNECT_API_KEY_PATH: ${{ secrets.APP_STORE_CONNECT_API_KEY_PATH }}
        run: bundle exec fastlane deploy_staging
```

### Fase 4: Configuración de Secrets

#### 4.1 Secrets Requeridos en GitHub
```bash
# Android
KEYSTORE_BASE64                    # Keystore codificado en base64
STORE_PASSWORD                     # Password del keystore
KEY_PASSWORD                       # Password de la key
KEY_ALIAS                         # Alias de la key
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON  # JSON de Google Play (base64)

# iOS
IOS_P12_CERT_BASE64               # Certificado P12 (base64)
IOS_P12_CERT_PASSWORD             # Password del certificado
IOS_PROVISION_PROFILE_BASE64      # Perfil de aprovisionamiento (base64)
APP_STORE_CONNECT_API_KEY_PATH    # Ruta a la API key de App Store Connect

# Opcionales para notificaciones
SLACK_WEBHOOK_URL                 # Para notificaciones de Slack
TEAMS_WEBHOOK_URL                 # Para notificaciones de Teams
```

### Fase 5: Mejoras Adicionales

#### 5.1 Notificaciones
```ruby
# Agregar a ambos Fastfiles
after_all do |lane|
  slack(
    message: "✅ Successfully deployed #{lane} build",
    slack_url: ENV['SLACK_WEBHOOK_URL']
  ) if ENV['SLACK_WEBHOOK_URL']
end

error do |lane, exception|
  slack(
    message: "❌ Failed to deploy #{lane}: #{exception.message}",
    slack_url: ENV['SLACK_WEBHOOK_URL'],
    success: false
  ) if ENV['SLACK_WEBHOOK_URL']
end
```

#### 5.2 Versionado Automático
```ruby
# Agregar lane para incrementar versiones
lane :bump_version do
  increment_build_number(xcodeproj: "Runner.xcodeproj") # iOS
  # Para Android, usar gradle plugin o manual en pubspec.yaml
end
```

## 🚀 Pasos de Implementación

### 1. Preparación
```bash
# Instalar dependencias de Ruby
cd android && bundle install
cd ../ios && bundle install

# Verificar configuración de Fastlane
bundle exec fastlane --version
```

### 2. Configurar Secrets en GitHub
- Ir a Settings > Secrets and variables > Actions
- Añadir todos los secrets listados arriba

### 3. Actualizar Fastfiles
- Reemplazar contenido de `android/fastlane/Fastfile`
- Reemplazar contenido de `ios/fastlane/Fastfile`

### 4. Crear ExportOptions.plist
- Crear archivos para staging y production en `ios/`

### 5. Actualizar GitHub Actions
- Modificar `.github/workflows/flutter-ci.yml`
- Añadir jobs de Fastlane

### 6. Testing
```bash
# Test local (desarrollo)
cd android && bundle exec fastlane build_development
cd ios && bundle exec fastlane build_development

# Test deployment (requiere secrets configurados)
# Se ejecutará automáticamente con push a dev/main
```

## 📊 Beneficios de esta Integración

### ✅ Ventajas
1. **Automatización completa** del deployment
2. **Consistencia** entre builds locales y CI/CD
3. **Rollback fácil** mediante Fastlane
4. **Notificaciones** automáticas de éxito/error
5. **Versionado** automático opcional
6. **Logs centralizados** y mejores
7. **Integración nativa** con stores

### ⚠️ Consideraciones
1. **Curva de aprendizaje** de Fastlane
2. **Configuración inicial** más compleja
3. **Dependencia** de Ruby/Bundler
4. **Secrets adicionales** requeridos

## 🔧 Mantenimiento

### Actualizaciones Regulares
```bash
# Actualizar Fastlane
bundle update fastlane

# Actualizar gems
bundle update

# Verificar configuración
bundle exec fastlane doctor
```

### Monitoreo
- Revisar logs de GitHub Actions regularmente
- Verificar builds en Play Console/App Store Connect
- Monitorear notificaciones de Slack/Teams

## 📚 Recursos Adicionales

- [Fastlane Documentation](https://docs.fastlane.tools/)
- [Flutter CI/CD Best Practices](https://docs.flutter.dev/deployment/cd)
- [GitHub Actions for Flutter](https://github.com/marketplace/actions/flutter-action)
- [Google Play Console API](https://developers.google.com/android-publisher)
- [App Store Connect API](https://developer.apple.com/app-store-connect/api/)