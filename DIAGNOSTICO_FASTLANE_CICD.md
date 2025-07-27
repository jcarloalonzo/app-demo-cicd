# 🚨 Diagnóstico: Configuración Fastlane + CI/CD - Flutter

## 📊 Estado Actual Detectado

### ❌ Problemas Identificados

#### 1. **Error de Sintaxis en Fastfile**
```ruby
# ❌ PROBLEMA en línea 29:
UI.success("✅ Keystore configurado correctamente"  # Falta paréntesis de cierre
```

#### 2. **Inconsistencia en Variables de Entorno**
```yaml
# ❌ En CI/CD:
PLAY_STORE_CONFIG_JSON: ${{ secrets.PLAY_STORE_CONFIG_JSON }}

# ❌ En Fastfile:  
json_key: ENV['PLAY_STORE_CONFIG_JSON']  # Variable diferente
```

#### 3. **Configuración de Ruby Incorrecta**
```yaml
# ❌ PROBLEMA: Instala Fastlane globalmente después de bundler-cache
- name: Setup Ruby
  uses: ruby/setup-ruby@v1
  with:
    bundler-cache: true  # Configura bundler cache
    working-directory: android

- name: Install Fastlane  
  run: gem install fastlane  # ❌ Instala globalmente, ignora Gemfile
```

#### 4. **Archivos JSON de Google Play**
```yaml
# ❌ No se crea el archivo físico, solo se pasa como ENV
env:
  PLAY_STORE_CONFIG_JSON: ${{ secrets.PLAY_STORE_CONFIG_JSON }}
```

#### 5. **Rutas de Keystore Incorrectas**
```ruby
# ❌ En Fastfile:
sh("mkdir", "-p", "../app")  # Debería ser "../android/app"
```

## 🛠️ Soluciones

### ✅ 1. Corregir Fastfile

```ruby
# android/fastlane/Fastfile
default_platform(:android)

platform :android do
  desc "Deploy staging App Bundle to Play Store Internal"
  lane :deploy_staging do
    setup_staging_keystore
    
    Dir.chdir("../") do
      sh("flutter", "build", "appbundle", "--flavor", "staging", "-t", "lib/main_staging.dart", "--release")
    end

    upload_to_play_store(
      track: 'internal',
      json_key: ENV['GOOGLE_PLAY_JSON_KEY_PATH'],
      package_name: 'com.jelafintegradores.democicd.staging',
      aab: '../build/app/outputs/bundle/stagingRelease/app-staging-release.aab'
    )
  end

  desc "Deploy production App Bundle to Play Store"
  lane :deploy_production do
    UI.message("🚀 Starting deploy_production lane")
    setup_production_keystore
    UI.success("✅ Keystore configurado correctamente")  # ✅ PARÉNTESIS CORREGIDO
    
    Dir.chdir("../") do
      UI.message("💻 Compilando App Bundle (flavor: production)…")
      sh("flutter", "build", "appbundle", "--flavor", "production", "-t", "lib/main_production.dart", "--release")
      UI.success("📦 App Bundle construido en build/app/outputs/bundle/productionRelease/")
    end

    UI.message("☁️ Subiendo a Google Play (track: production)…")
    upload_to_play_store(
      track: 'production', 
      json_key: ENV['GOOGLE_PLAY_JSON_KEY_PATH'],  # ✅ VARIABLE CORREGIDA
      package_name: 'com.jelafintegradores.democicd',
      aab: '../build/app/outputs/bundle/productionRelease/app-production-release.aab'
    )
    UI.success("🎉 ¡Upload completado!")
  end

  private_lane :setup_staging_keystore do
    UI.message("🔧 Configurando keystore para staging...")
    sh("mkdir", "-p", "../android/app")  # ✅ RUTA CORREGIDA
    sh("echo", "#{ENV['KEYSTORE_BASE64']}", "|", "base64", "-d", ">", "../android/app/staging-keystore.jks")
    
    File.open("../android/key.staging.properties", "w") do |file|
      file.puts "storePassword=#{ENV['STORE_PASSWORD']}"
      file.puts "keyPassword=#{ENV['KEY_PASSWORD']}"
      file.puts "keyAlias=#{ENV['KEY_ALIAS']}"
      file.puts "storeFile=staging-keystore.jks"
    end
    UI.success("✅ Staging keystore configurado")
  end

  private_lane :setup_production_keystore do
    UI.message("🔧 Configurando keystore para production...")
    sh("mkdir", "-p", "../android/app")  # ✅ RUTA CORREGIDA
    sh("echo", "#{ENV['KEYSTORE_BASE64']}", "|", "base64", "-d", ">", "../android/app/production-keystore.jks")
    
    File.open("../android/key.production.properties", "w") do |file|
      file.puts "storePassword=#{ENV['STORE_PASSWORD']}"
      file.puts "keyPassword=#{ENV['KEY_PASSWORD']}"
      file.puts "keyAlias=#{ENV['KEY_ALIAS']}"
      file.puts "storeFile=production-keystore.jks"
    end
    UI.success("✅ Production keystore configurado")
  end
end
```

### ✅ 2. Corregir GitHub Actions Workflow

```yaml
# .github/workflows/flutter-ci.yml (build-production job)
build-production:
  name: Build Production APK
  needs: prepare
  runs-on: ubuntu-latest
  environment: production
  
  steps:
    - name: Checkout repos
      uses: actions/checkout@v4

    - name: Setup Java
      uses: actions/setup-java@v4
      with:
        distribution: "temurin"
        java-version: ${{ env.JAVA_VERSION }}

    - name: Setup Flutter
      uses: subosito/flutter-action@v2
      with:
        flutter-version: ${{ env.FLUTTER_VERSION }}
        channel: ${{ env.FLUTTER_CHANNEL }}

    - name: Download dependencies
      uses: actions/download-artifact@v4
      with:
        name: flutter-dependencies

    # ✅ CORRECCIÓN: Setup Ruby CORRECTAMENTE
    - name: Setup Ruby
      uses: ruby/setup-ruby@v1
      with:
        ruby-version: "3.2"
        bundler-cache: true
        working-directory: android

    # ✅ CORRECCIÓN: Crear archivo JSON físico de Google Play
    - name: Setup Google Play Service Account
      run: |
        echo "${{ secrets.PLAY_STORE_CONFIG_JSON }}" | base64 -d > android/google-play-key.json

    # ✅ CORRECCIÓN: Variables de entorno unificadas
    - name: Run Fastlane Production Deploy
      working-directory: android
      env:
        KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        GOOGLE_PLAY_JSON_KEY_PATH: google-play-key.json  # ✅ NOMBRE CONSISTENTE
      run: bundle exec fastlane deploy_production  # ✅ Usar bundler
```

### ✅ 3. Verificar Gemfile

```ruby
# android/Gemfile
source "https://rubygems.org"

gem "fastlane"
gem "fastlane-plugin-supply"  # Para Google Play uploads
```

### ✅ 4. Secrets Requeridos en GitHub

```bash
# ✅ Secrets necesarios:
KEYSTORE_BASE64              # Keystore codificado en base64
STORE_PASSWORD               # Password del keystore
KEY_PASSWORD                 # Password de la key
KEY_ALIAS                   # Alias de la key
PLAY_STORE_CONFIG_JSON      # JSON de Google Play Service Account (base64)
```

## 🎯 Configuración Completa Recomendada

### Estructura de Archivos:
```
android/
├── fastlane/
│   ├── Fastfile            # ✅ Configuración corregida
│   └── Appfile             # Opcional
├── Gemfile                 # ✅ Con fastlane
└── Gemfile.lock           # Generado automáticamente
```

### Package Names Consistentes:
```ruby
# Verificar que coincidan con tu configuración real:
staging: 'com.jelafintegradores.democicd.staging'
production: 'com.jelafintegradores.democicd'
```

## 🚀 Pasos de Implementación

### 1. Corregir Fastfile
```bash
# Aplicar las correcciones de sintaxis y rutas
```

### 2. Actualizar GitHub Actions
```bash
# Usar las configuraciones corregidas del workflow
```

### 3. Configurar Secrets
```bash
# Añadir todos los secrets necesarios en GitHub
```

### 4. Test Local (Opcional)
```bash
cd android
bundle install
bundle exec fastlane deploy_production --env development  # Para testing
```

## ⚠️ Problemas Potenciales Adicionales

1. **Package Names**: Verificar que coincidan exactamente con Google Play Console
2. **Signing Config**: Asegurar que `build.gradle` tenga configuración para flavors
3. **Permissions**: Verificar que la Service Account tenga permisos de upload
4. **Track Availability**: Confirmar que el track 'production' esté disponible

## 📋 Checklist Final

- [ ] ✅ Fastfile con sintaxis corregida
- [ ] ✅ Variables de entorno consistentes  
- [ ] ✅ Archivo JSON de Google Play creado correctamente
- [ ] ✅ Ruby configurado con bundler
- [ ] ✅ Rutas de keystore corregidas
- [ ] ✅ Package names verificados
- [ ] ✅ Secrets configurados en GitHub
- [ ] ✅ Gemfile con dependencias necesarias