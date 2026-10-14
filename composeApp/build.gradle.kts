import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation("androidx.core:core-ktx:1.15.0")
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "dev.namaa.money"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.zimma.money"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.4.0"
    }
    signingConfigs {
        create("release") {
            // Every scheme: v1 for old Android, v2 and v3 (which lets the key be rotated later) and v4 for streamed installs.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
            val store = System.getenv("NAMAA_KEYSTORE")
            if (store != null) {
                storeFile = file(store)
                storePassword = System.getenv("NAMAA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NAMAA_KEY_ALIAS") ?: "zimma"
                keyPassword = System.getenv("NAMAA_KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("NAMAA_KEYSTORE") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

compose.desktop {
    application {
        mainClass = "dev.namaa.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "namaa"
            packageVersion = "1.0.0"
            description = "Private money tracking in plain files"
            vendor = "Zimam"
            linux {
                menuGroup = "Utility"
                appCategory = "Utility"
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
        }
    }
}
