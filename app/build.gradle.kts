import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace  = "com.horas_al_mando.ham_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.horas_al_mando.ham_android"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] =
            localProps.getProperty("MAPS_API_KEY", "YOUR_MAPS_API_KEY")

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${localProps.getProperty("API_BASE_URL", "https://api.horasalmando.com.ar/api/v1/")}\""
        )
        buildConfigField(
            "String",
            "WS_BASE_URL",
            "\"${localProps.getProperty("WS_BASE_URL", "wss://api.horasalmando.com.ar/api")}\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Auth & Security
    implementation(libs.androidx.security.crypto)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    debugImplementation(libs.compose.ui.tooling)

}
