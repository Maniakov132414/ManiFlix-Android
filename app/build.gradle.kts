plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun readSigningValue(propName: String, vararg envNames: String): String? {
    val prop = providers.gradleProperty(propName).orNull?.takeIf { it.isNotBlank() }
    if (prop != null) return prop
    for (envName in envNames) {
        val envVal = System.getenv(envName)?.takeIf { it.isNotBlank() }
        if (envVal != null) return envVal
    }
    return null
}

val releaseKeystorePath = readSigningValue(
    "maniflix.keystore.path",
    "MANIFLIX_KEYSTORE_PATH",
    "ANDROID_KEYSTORE_PATH",
    "KEYSTORE_PATH"
)
val releaseKeystorePassword = readSigningValue(
    "maniflix.keystore.password",
    "MANIFLIX_KEYSTORE_PASSWORD",
    "ANDROID_KEYSTORE_PASSWORD",
    "KEYSTORE_PASSWORD"
)
val releaseKeyAlias = readSigningValue(
    "maniflix.key.alias",
    "MANIFLIX_KEY_ALIAS",
    "ANDROID_KEY_ALIAS",
    "KEY_ALIAS"
)
val releaseKeyPassword = readSigningValue(
    "maniflix.key.password",
    "MANIFLIX_KEY_PASSWORD",
    "ANDROID_KEY_PASSWORD",
    "KEY_PASSWORD"
)
val hasReleaseSigning = releaseKeystorePath != null &&
    file(releaseKeystorePath).exists() &&
    releaseKeystorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "bond.maniakov.maniflix"
    compileSdk = 35

    defaultConfig {
        applicationId = "bond.maniakov.maniflix"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
