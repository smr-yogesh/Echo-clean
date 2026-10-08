import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.gms) apply false
    alias(libs.plugins.crashlytics) apply false
}



val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val hasGoogleServices = file("google-services.json").exists()
val gitHash = "0000000"
val gitCount = 1
val version = "3.0.$gitCount"

android {
    namespace = "com.cleanui.echo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cleanui.echo"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.3"
    }

    signingConfigs {
        // Keep your debug config for emulator testing
        getByName("debug") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
        }

        // Add production release signing config
        create("stable") {
            storeFile = file(localProperties.getProperty("KEYSTORE_FILE") ?: "")
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }

        create("stable") {
            isDebuggable = false // Removes debug flags
            isMinifyEnabled = true // Obfuscates code & removes unused code
            isShrinkResources = true // Shrinks unused resources

            // Assigns stable signing config
            signingConfig = signingConfigs.getByName("stable")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.androidx)
    implementation(libs.material)
    implementation(libs.bundles.paging)
    implementation(libs.filekache)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.media3)
    implementation(libs.bundles.coil)

    implementation(libs.pikolo)
    implementation(libs.fadingedgelayout)
    implementation(libs.fastscroll)
    implementation(libs.kenburnsview)
    implementation(libs.nestedscrollwebview)
    implementation(libs.acsbendi.webview)

    if (!hasGoogleServices) return@dependencies
    implementation(libs.bundles.firebase)
}

if (hasGoogleServices) {
    apply(plugin = libs.plugins.gms.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
}

fun execute(vararg command: String): String = providers.exec {
    commandLine(*command)
}.standardOutput.asText.get().trim()