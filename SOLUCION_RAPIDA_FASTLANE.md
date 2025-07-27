# 🔧 Solución Rápida - Fastlane CI/CD

## ❌ Problemas Actuales Detectados

### 1. **Fastfile - Variable json_key incorrecta**
```ruby
# ❌ LÍNEA 44:
json_key: ENV['PLAY_STORE_CONFIG_JSON']  # Busca variable, no archivo
```

### 2. **Fastfile - Rutas de keystore incorrectas**
```ruby
# ❌ LÍNEAS 58-59:
sh("mkdir", "-p", "../app")  # Ruta incorrecta
sh("echo", ENV['KEYSTORE_BASE64'], "|", "base64", "-d", ">", "../app/production-keystore.jks")
```

### 3. **CI/CD - Conflicto Ruby/Fastlane**
```yaml
# ❌ LÍNEAS 177-186:
bundler-cache: true        # Configura bundler
gem install fastlane      # Instala globalmente (conflicto)
```

### 4. **CI/CD - Comando Fastlane incorrecto**
```yaml
# ❌ LÍNEA 220:
run: fastlane android deploy_production  # Sintaxis incorrecta
```

## ✅ CORRECCIONES ESPECÍFICAS

### 📝 1. Actualizar Fastfile

**Cambiar línea 44:**
```ruby
# DE:
json_key: ENV['PLAY_STORE_CONFIG_JSON']

# A:
json_key: 'playstore-credential.json'
```

**Cambiar líneas 58-59:**
```ruby
# DE:
sh("mkdir", "-p", "../app")
sh("echo", ENV['KEYSTORE_BASE64'], "|", "base64", "-d", ">", "../app/production-keystore.jks")

# A:
sh("mkdir", "-p", "../android/app")
sh("echo", ENV['KEYSTORE_BASE64'], "|", "base64", "-d", ">", "../android/app/production-keystore.jks")
```

**Cambiar línea 61:**
```ruby
# DE:
File.open("../key.production.properties", "w") do |file|

# A:
File.open("../android/key.production.properties", "w") do |file|
```

### 📝 2. Actualizar GitHub Actions

**Eliminar líneas 184-186:**
```yaml
# ELIMINAR:
- name: Install Fastlane
  run: gem install fastlane
```

**Cambiar línea 220:**
```yaml
# DE:
run: fastlane android deploy_production

# A:
run: bundle exec fastlane deploy_production
working-directory: android
```

## 🚀 Fastfile Completo Corregido

```ruby
default_platform(:android)

platform :android do
  desc "Deploy production App Bundle to Play Store"
  lane :deploy_production do
    UI.message("🚀 Starting deploy_production lane")
    setup_production_keystore
    UI.success("✅ Keystore configurado correctamente")
    
    # Build App Bundle
    Dir.chdir("../") do
      UI.message("💻 Compilando App Bundle (flavor: production)…")
      sh("flutter build appbundle --flavor production -t lib/main_production.dart --release")
      UI.success("📦 App Bundle construido en build/app/outputs/bundle/productionRelease/")
    end

    UI.message("☁️ Subiendo a Google Play (track: production)…")
    upload_to_play_store(
      track: 'production', 
      json_key: 'playstore-credential.json',  # ✅ CORREGIDO
      package_name: 'com.jelafintegradores.democicd',
      aab: '../build/app/outputs/bundle/productionRelease/app-production-release.aab',
      skip_upload_metadata: true,
      skip_upload_images: true,
      skip_upload_screenshots: true,
      skip_upload_changelogs: true
    )
    UI.success("🎉 ¡Upload completado!")
  end

  private_lane :setup_production_keystore do
    UI.message("🔧 Configurando keystore para production...")
    sh("mkdir", "-p", "../android/app")  # ✅ CORREGIDO
    sh("echo", ENV['KEYSTORE_BASE64'], "|", "base64", "-d", ">", "../android/app/production-keystore.jks")  # ✅ CORREGIDO
    
    File.open("../android/key.production.properties", "w") do |file|  # ✅ CORREGIDO
      file.puts "storePassword=#{ENV['STORE_PASSWORD']}"
      file.puts "keyPassword=#{ENV['KEY_PASSWORD']}"
      file.puts "keyAlias=#{ENV['KEY_ALIAS']}"
      file.puts "storeFile=production-keystore.jks"
    end
    UI.success("✅ Production keystore configurado")
  end
end
```

## 🚀 GitHub Actions Corregido

```yaml
build-production:
  name: Build Production APK
  needs: prepare
  runs-on: ubuntu-latest
  environment: production
  
  steps:
    - name: Checkout repos
      uses: actions/checkout@v4

    - name: Setup Ruby
      uses: ruby/setup-ruby@v1
      with:
        ruby-version: "3.2"
        bundler-cache: true
        working-directory: android

    # ✅ ELIMINADO: Install Fastlane global

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

    - name: Write Play store credential
      working-directory: android
      run: |
        echo "${{ secrets.PLAY_STORE_CONFIG_JSON }}" | base64 -d > playstore-credential.json
        echo "PLAY_STORE_CONFIG_JSON file created successfully."
        pwd
        ls -la
        ls

    - name: Run Fastlane lane
      working-directory: android  # ✅ AÑADIDO
      env:
        KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
      run: bundle exec fastlane deploy_production  # ✅ CORREGIDO
```

## 📋 Checklist de Cambios

- [ ] ✅ Cambiar `json_key` en Fastfile línea 44
- [ ] ✅ Corregir rutas de keystore líneas 58-59  
- [ ] ✅ Corregir ruta de properties línea 61
- [ ] ✅ Eliminar `gem install fastlane` del CI/CD
- [ ] ✅ Cambiar comando a `bundle exec fastlane deploy_production`
- [ ] ✅ Añadir `working-directory: android` al step final

## 🎯 Resultado Esperado

Con estas correcciones:
1. Fastlane usará el archivo JSON físico correctamente
2. Los keystores se crearán en las rutas correctas
3. Ruby/Bundler funcionarán sin conflictos
4. El comando se ejecutará desde el directorio correcto

## ⚠️ Verificar Después

1. **Secrets configurados**: Todos los secrets deben estar en GitHub
2. **Package name**: Verificar que coincida con Google Play Console
3. **Gemfile**: Debe existir en `android/Gemfile` con `gem "fastlane"`