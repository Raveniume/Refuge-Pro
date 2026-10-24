import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val releaseVersion = Properties().also { properties ->
    rootProject.file("version.properties").inputStream().use { stream ->
        properties.load(stream)
    }
}

val signingProperties = Properties().also { properties ->
    rootProject.file("signing.properties").takeIf { it.isFile }?.inputStream()?.use { stream ->
        properties.load(stream)
    }
}

fun signingValue(name: String): String? =
    providers.gradleProperty(name).orNull
        ?: signingProperties.getProperty(name)

val releaseStoreFile = signingValue("storeFile")
val releaseStorePassword = signingValue("storePassword")
val releaseKeyAlias = signingValue("keyAlias")
val releaseKeyPassword = signingValue("keyPassword")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.refuge.next"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.refuge.next.compose"
        minSdk = 29
        targetSdk = 36
        versionCode = releaseVersion.getProperty("versionCode").toInt()
        versionName = releaseVersion.getProperty("versionName")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("refugeRelease") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    // Keep the standard per-user debug keystore explicit so every desktop APK
    // remains upgrade-compatible during phone testing.
    buildTypes {
        getByName("debug") {
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("refugeRelease") else signingConfigs.getByName("debug")
        }
        create("review") {
            initWith(getByName("release"))
            // Local performance/visual review stays compatible with installed user data.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("refugeRelease") else signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            matchingFallbacks += listOf("release")
        }
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("refugeRelease")
            }
        }
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
            "META-INF/NOTICE.md",
            "META-INF/DEPENDENCIES"
        )
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("zone.ien.hig:hig:1.4.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(compose.runtime)
    implementation(compose.animation)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.ui)
    implementation(compose.materialIconsExtended)
    implementation("io.github.kyant0:backdrop:2.0.0")
    implementation("io.github.kyant0:shapes:1.2.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.21.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.11.1")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.11.1")
}
