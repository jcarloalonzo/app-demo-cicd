import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

val keystoreTestProperties = Properties()
val keystoreTestPropertiesFile = rootProject.file("key.staging.properties")
if (keystoreTestPropertiesFile.exists()) {
    keystoreTestProperties.load(FileInputStream(keystoreTestPropertiesFile))
}

val keystoreProdProperties = Properties()
val keystoreProdPropertiesFile = rootProject.file("key.production.properties")
if (keystoreProdPropertiesFile.exists()) {
    keystoreProdProperties.load(FileInputStream(keystoreProdPropertiesFile))
}

android {
    namespace = "com.jelafintegradores.democicd"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "com.jelafintegradores.democicd"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        // Configuración de debug por defecto.
        // create("debug") { }

        if(keystoreTestPropertiesFile.exists()){
            create("releaseStaging") {
                val storeFilePath = keystoreTestProperties["storeFile"] as String
                val keyAliasValue = keystoreTestProperties["keyAlias"] as String
                
                println("=== STAGING SIGNING CONFIG ===")
                println("storeFile path: $storeFilePath")
                println("storeFile exists: ${file(storeFilePath).exists()}")
                println("keyAlias: $keyAliasValue")
                println("==============================")
                
                keyAlias = keyAliasValue
                keyPassword = keystoreTestProperties["keyPassword"] as String
                storeFile = file(storeFilePath)
                storePassword = keystoreTestProperties["storePassword"] as String
            }
        }
        if(keystoreProdPropertiesFile.exists()){
            create("releaseProduction") {
                keyAlias = keystoreProdProperties["keyAlias"] as String
                keyPassword = keystoreProdProperties["keyPassword"] as String
                storeFile = file(keystoreProdProperties["storeFile"] ?: "")
                storePassword = keystoreProdProperties["storePassword"] as String
            }
        }
        println("=== DEBUG KEYSTORE  🚀🚀🚀🚀🚀🚀🚀===")
  


    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            // La firma se asignará dinámicamente según el flavor.
        }
    }

    flavorDimensions += "environment"


    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            versionCode = 1
            versionName = "0.0.1"
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            versionCode = 4
            versionName = "0.0.4"
                    // Mover esto AQUÍ, fuera de buildTypes
            if (keystoreTestPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("releaseStaging")
            }
        }
        create("production") {
            dimension = "environment"
            applicationIdSuffix = ""
            versionNameSuffix = ""
            versionCode = 6
            versionName = "0.0.6"
            // Mover esto AQUÍ también
            if (keystoreProdPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("releaseProduction")
            }
        }
    }
}

flutter {
    source = "../.."
}
