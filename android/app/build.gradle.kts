import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "org.justquran.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.justquran.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 82
        versionName = "2.2.67"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    val propFile = rootProject.file("signing.properties")
    if (propFile.exists()) {
        signingConfigs {
            create("release") {
                val props = Properties().apply {
                    propFile.inputStream().use { load(it) }
                }
                val ksPath = props.getProperty("storeFile") ?: "keystore/munajaat-release.jks"
                val ks = rootProject.file(ksPath)
                if (ks.exists()) {
                    storeFile = ks
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias") ?: "munajaat"
                    keyPassword = props.getProperty("keyPassword")
                    enableV1Signing = true
                    enableV2Signing = true
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let {
                if (it.storeFile?.exists() == true) {
                    signingConfig = it
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
