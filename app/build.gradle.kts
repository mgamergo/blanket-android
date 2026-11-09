plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.spotless)
}

android {
    namespace = "org.blanket.android"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "org.blanket.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.8.0-android.1"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = "2048m"
            it.maxParallelForks = 1
            it.systemProperty("robolectric.graphicsMode", "NATIVE")
            if (
                !providers
                    .gradleProperty("roborazzi.test.record")
                    .map(String::toBoolean)
                    .getOrElse(false)
            ) {
                it.systemProperty("roborazzi.test.verify", "true")
            }
            it.systemProperty(
                "robolectric.dependency.repo.url",
                "https://repo.maven.apache.org/maven2",
            )
        }
    }
    androidResources { noCompress += "ogg" }
    lint {
        abortOnError = true
        warningsAsErrors = true
        // Dependency updates are reviewed separately from reproducible correctness checks.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktfmt("0.58").kotlinlangStyle()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt("0.58").kotlinlangStyle()
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.lifecycle)
    implementation(libs.datastore)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.coroutines)
    implementation(libs.androidsvg)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
