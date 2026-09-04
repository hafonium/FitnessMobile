import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

// Groq API key: read from local.properties (gitignored, never committed) and baked into
// BuildConfig at build time. Each dev must add their own `GROQ_API_KEY=...` line locally -
// see docs/chatbot-feature.md. The fallback default here must stay "" (never the real key) since
// this file, unlike local.properties, is committed to git.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val groqApiKey: String = localProperties.getProperty("GROQ_API_KEY", "")
val stadiaMapsApiKey: String = providers.environmentVariable("STADIA_MAPS_API_KEY").orNull
    ?: localProperties.getProperty("STADIA_MAPS_API_KEY", "")

android {
    namespace = "com.example.homeworkout"
    // Bumped from 36.1 to 37: the Compose Markdown renderer's AAR metadata (multiplatform-
    // markdown-renderer(-m3)-android:0.45.0) requires compileSdk 37+. This only affects which
    // platform APIs the app can compile against - targetSdk/minSdk (actual runtime behavior,
    // device compatibility) are intentionally left untouched, per AGP's own guidance for this.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.homeworkout"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
        val escapedStadiaMapsApiKey = stadiaMapsApiKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "STADIA_MAPS_API_KEY", "\"$escapedStadiaMapsApiKey\"")
        val localProperties = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use(::load)
            }
        }
        val spoonacularApiKey = providers.environmentVariable("SPOONACULAR_API_KEY").orNull
            ?: localProperties.getProperty("SPOONACULAR_API_KEY", "")
        val escapedApiKey = spoonacularApiKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$escapedApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.lint)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.test)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    testImplementation(libs.junit)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-extended")

    // Room (local persistence)
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion") // For Coroutines/Flow support
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coil — async image loading for exercise photos and GIF demos (image_url / gif_url from the seed data)
    val coilVersion = "2.7.0"
    implementation("io.coil-kt:coil-compose:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")

    // Reorderable list
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // OkHttp - direct-from-app HTTP client for the Groq chat assistant (no backend; see
    // docs/chatbot-feature.md). This is the first remote/API dependency in the project.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Compose Markdown renderer (Material3-themed) - renders LLM chat replies (bold, lists,
    // code, links) instead of showing raw markdown syntax. Pure Compose, no WebView/TextView
    // interop. See docs/chatbot-feature.md.
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.45.0")

    // OpenGL is the widest-compatible stable MapLibre backend for minSdk 24 devices.
    implementation("org.maplibre.gl:android-sdk-opengl:13.4.1")
}

// The Markdown renderer above was published against a newer Kotlin release than this project
// uses, and its POM pulls in a newer kotlin-stdlib transitively; Gradle resolves to the highest
// version by default, and this project's Kotlin 2.2.10 compiler can't read class metadata from
// that newer stdlib jar ("class was compiled with an incompatible version of Kotlin"), which
// breaks compilation across the whole module, not just the new dependency. Kotlin's stdlib is
// strongly backward-compatible at the API level, so pinning every org.jetbrains.kotlin artifact
// back to the project's own version is safe and is the standard fix for this class of error.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.2.10")
        }
    }
}
