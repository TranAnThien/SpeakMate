import com.android.build.api.variant.BuildConfigField

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val geminiKeyName = "GEMINI_API_KEY"

fun resolveGeminiApiKey(): String {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        val assignment = Regex("""^\s*(?:export\s+)?GEMINI_API_KEY\s*=\s*(?:"([^"]*)"|'([^']*)'|([^#\s]*))\s*(?:#.*)?$""")
        envFile.readLines().forEach { line ->
            assignment.matchEntire(line.trim())?.destructured?.let { (dq, sq, uq) ->
                val key = dq.ifEmpty { sq }.ifEmpty { uq }.trim()
                if (key.isNotEmpty()) return key
            }
        }
    }
    val localPropFile = rootProject.file("local.properties")
    if (localPropFile.exists()) {
        localPropFile.readLines().forEach { line ->
            if (line.trim().startsWith("GEMINI_API_KEY=")) {
                val key = line.substringAfter("GEMINI_API_KEY=").trim().trim('"', '\'')
                if (key.isNotEmpty()) return key
            }
        }
    }
    return System.getenv("GEMINI_API_KEY")?.trim().orEmpty()
}

android {
    namespace = "com.project.speakmate"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.project.speakmate"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val resolvedKey = resolveGeminiApiKey()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", geminiKeyName, "\"$resolvedKey\"")
    }

    buildTypes {
        debug {
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.generativeai)
    implementation(libs.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("io.mockk:mockk:1.13.12")
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.okhttp)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.viewmodel.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation(libs.androidx.compose.material.icons.extended)
}
