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
        if(keystoreTestPropertiesFile.exists()){
            create("releaseStaging") {
                keyAlias = keystoreTestProperties["keyAlias"] as String
                keyPassword = keystoreTestProperties["keyPassword"] as String
                storeFile = file(keystoreTestProperties["storeFile"] ?: "")
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

    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("releaseProduction")
            // signingConfig = signingConfigs.getByName("releaseStaging")
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
            versionCode = 1
            versionName = "0.0.1"
            //signingConfig = signingConfigs.getByName("releaseStaging")
            buildTypes {
                getByName("debug") {
                    signingConfig = signingConfigs.getByName("debug")
                }
                getByName("release") {
                    signingConfig = if (keystoreTestPropertiesFile.exists())
                        signingConfigs.getByName("releaseStaging")
                    else
                        signingConfigs.getByName("debug")
                }
            }
        }
        create("production") {
            dimension = "environment"
            applicationIdSuffix = ".app"
            versionNameSuffix = ""
            versionCode = 1
            versionName = "0.0.1"
            //signingConfig = signingConfigs.getByName("releaseProduction")
            buildTypes {
                getByName("debug") {
                    signingConfig = signingConfigs.getByName("debug")
                }
                getByName("release") {
                    signingConfig = if (keystoreProdPropertiesFile.exists())
                        signingConfigs.getByName("releaseProduction")
                    else
                        signingConfigs.getByName("debug")
                }
            }

        }
    }
}

flutter {
    source = "../.."
}
