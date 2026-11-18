import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// PAYMENT_BASE_URL dibaca dari local.properties (tidak di-commit). Jika tidak ada,
// dipakai alamat laptop dari emulator, sehingga build di CI tetap berhasil.
val localProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull?.let { load(it.reader()) }
}
val paymentBaseUrl: String = localProperties.getProperty("PAYMENT_BASE_URL")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let { if (it.endsWith("/")) it else "$it/" }   // Retrofit mewajibkan akhiran "/"
    ?: "http://10.0.2.2:3000/"

android {
    namespace = "com.example.unimarket"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.unimarket"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PAYMENT_BASE_URL", "\"$paymentBaseUrl\"")
    }

    buildTypes {
        debug {
            // (Bonus) laporan coverage unit test: ./gradlew createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    
    // Room components
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Image loading (Coil)
    implementation(libs.coil.compose)

    // Jaringan: payment-server (Midtrans sandbox)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // ── Unit test (src/test, JVM) ──────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // ── Instrumented test (src/androidTest, emulator/HP) ───────────────────
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}