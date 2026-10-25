plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

/**
 * Which CPU architectures to pack into the APK.
 * The on-device yt-dlp engine ships a Python runtime + ffmpeg per ABI,
 * so we only ship the ones real phones use. Override with:
 *   ./gradlew assembleDebug -Pabis=arm64-v8a,armeabi-v7a,x86_64
 */
val targetAbis: List<String> = (project.findProperty("abis") as String?)
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: listOf("arm64-v8a", "armeabi-v7a")

val keystorePath: String? = System.getenv("AURALIS_KEYSTORE")

android {
    namespace = "dev.auralis"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.auralis"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }

        ndk {
            abiFilters += targetAbis
        }
    }

    signingConfigs {
        if (keystorePath != null) {
            create("auralisRelease") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("AURALIS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("AURALIS_KEY_ALIAS")
                keyPassword = System.getenv("AURALIS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Signed with your keystore when AURALIS_KEYSTORE is set, otherwise
            // falls back to the debug key so CI always produces an installable APK.
            signingConfig = signingConfigs.findByName("auralisRelease")
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // youtubedl-android unpacks its Python runtime from native libs
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Playback
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")

    // Library storage
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // 100% on-device yt-dlp (bundled python + ffmpeg)
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
}
