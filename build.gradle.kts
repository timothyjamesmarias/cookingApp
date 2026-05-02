plugins {
    kotlin("jvm") version "2.3.0" apply false
    kotlin("multiplatform") version "2.3.0" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    kotlin("plugin.compose") version "2.3.0" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
    id("org.jetbrains.compose.hot-reload") version "1.0.0" apply false
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
}

group = "com.timothymarias"
version = "0.0.1-SNAPSHOT"
description = "cooking-app"

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version
    
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}
