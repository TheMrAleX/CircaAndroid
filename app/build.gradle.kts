import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}
android {
    namespace = "es.aproximado.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "es.aproximado.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.0.8"
        ndk { abiFilters += "arm64-v8a" }
    }
    signingConfigs {
        create("release") {
            val path = System.getenv("KEYSTORE_PATH")
            if (path != null) {
                storeFile = file(path)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint { abortOnError = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            val testHome = layout.buildDirectory.dir("test-home").get().asFile.apply { mkdirs() }
            test.systemProperty("user.home", testHome.absolutePath)
            test.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            System.getenv("HTTPS_PROXY")?.let { proxy ->
                val uri = URI(proxy)
                listOf("https.proxyHost", "http.proxyHost", "robolectric.dependency.proxy.host").forEach { test.systemProperty(it, uri.host) }
                listOf("https.proxyPort", "http.proxyPort", "robolectric.dependency.proxy.port").forEach { test.systemProperty(it, uri.port.toString()) }
                val trust = file("/etc/ssl/certs/java/cacerts")
                if (trust.exists()) test.systemProperty("javax.net.ssl.trustStore", trust.absolutePath)
            }
        }
    }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutinestest)
}
