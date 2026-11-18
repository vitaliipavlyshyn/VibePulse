import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.skie)
    alias(libs.plugins.buildkonfig)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

buildkonfig {
    packageName = "com.vpch.vibepulse"

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "SPOTIFY_CLIENT_ID",
            localProperties.getProperty("SPOTIFY_CLIENT_ID") ?: ""
        )
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "OPENWEATHER_API_KEY",
            value = localProperties.getProperty("OPENWEATHER_API_KEY") ?: ""
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SPOTIFY_BASE_URL",
            "https://api.spotify.com/v1/"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SPOTIFY_AUTH_BASE_URL",
            "https://accounts.spotify.com/"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "RECCOBEATS_BASE_URL",
            "https://api.reccobeats.com/v1/"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "OPENWEATHER_BASE_URL",
            "https://api.openweathermap.org/data/2.5/"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "OPENWEATHER_GEO_BASE_URL",
            "https://api.openweathermap.org/geo/1.0/"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SPOTIFY_REDIRECT_URI",
            "vibepulse://spotify-callback"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SPOTIFY_PACKAGE_NAME",
            "com.spotify.music"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SPOTIFY_TRACK_URI_PREFIX",
            "spotify:track:"
        )
        buildConfigField(
            FieldSpec.Type.STRING,
            "SPOTIFY_OPEN_TRACK_BASE_URL",
            "https://open.spotify.com/track/"
        )
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
            export(libs.androidx.lifecycle.viewmodelCompose)
        }
    }
    android {
       namespace = "com.vpch.vibepulse.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.play.services.location)
            implementation(":spotify-app-remote-release-0.8.0@aar")
            implementation(libs.gson)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            api(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.multiplatform.settings)
            implementation(libs.ktor.client.auth)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}


dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    add("kspAndroid", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
}