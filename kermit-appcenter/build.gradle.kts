plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
}

val APPCENTER_ANDROID_VERSION: String by project

apply(from = "../gradle/configure-crash-logger.gradle")

group = "org.burnoutcrew"
version = "0.0.1"

kotlin {
    android {
        publishAllLibraryVariants()
    }

    val androidMain by sourceSets.getting {
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib")
            implementation("com.microsoft.appcenter:appcenter-crashes:$APPCENTER_ANDROID_VERSION")
        }
    }
}

android {
    namespace = "org.burnoutcrew.kermit.appcenter"
    compileSdk = 32
    defaultConfig {
        minSdk = 15
        targetSdk = 32
    }

    val main by sourceSets.getting
}