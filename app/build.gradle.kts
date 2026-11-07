import com.android.build.api.variant.BuildConfigField

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val geminiKeyName = "GEMINI_API_KEY"

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
        buildConfigField("String", geminiKeyName, "\"\"")
    }

    buildTypes {
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

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val keyName = geminiKeyName
        val assignment = Regex("""\s*(?:export\s+)?$keyName\s*=\s*(?:"([^"]*)"|'([^']*)'|([^#\s]*))\s*(?:#.*)?""")
        val dotenv = rootProject.layout.projectDirectory.file(".env")
        val geminiApiKey = providers.fileContents(dotenv).asText.map { contents ->
            contents.removePrefix("\uFEFF").lineSequence().mapNotNull { line ->
                assignment.matchEntire(line)?.destructured?.let { (doubleQuoted, singleQuoted, unquoted) ->
                    doubleQuoted.ifEmpty { singleQuoted }.ifEmpty { unquoted }.trim()
                }
            }.lastOrNull().orEmpty()
        }.filter(String::isNotEmpty)
            .orElse(providers.environmentVariable(keyName))
            .orElse("")
            .map { key ->
                val escaped = key.trim().replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\r", "\\r").replace("\n", "\\n")
                BuildConfigField("String", "\"$escaped\"", null)
            }
        checkNotNull(variant.buildConfigFields).put(keyName, geminiApiKey)
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
}
