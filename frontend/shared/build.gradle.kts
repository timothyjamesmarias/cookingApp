plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.2.1"
}

kotlin {
    androidTarget()
    
    jvm("desktop")
    
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )
    
    sourceSets { 
        val commonMain by getting {
            dependencies {
                implementation(project(":shared:domain"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation("io.ktor:ktor-client-core:3.3.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
                implementation("io.ktor:ktor-client-logging:3.3.3")
                implementation("app.softwork:kotlinx-uuid-core:0.0.22")
                implementation("io.insert-koin:koin-core:4.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

                // SQLDelight common runtime + coroutines extensions
                implementation("app.cash.sqldelight:runtime:2.2.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.2.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
                implementation("app.cash.turbine:turbine:1.0.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("io.ktor:ktor-client-android:3.3.3")

                // SQLDelight Android driver
                implementation("app.cash.sqldelight:android-driver:2.2.1")
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-okhttp:3.3.3")

                // SQLDelight JVM/desktop driver
                implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("app.cash.sqldelight:sqlite-driver:2.2.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
                implementation("app.cash.turbine:turbine:1.0.0")
            }
        }

        val iosX64Main by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.2.1")
            }
        }
        val iosArm64Main by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.2.1")
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.2.1")
            }
        }
    }
}

sqldelight {
    databases {
        create(name = "CookingDatabase") {
            packageName.set("com.timothymarias.cookingapp.shared.db")
            deriveSchemaFromMigrations.set(true)
        }
    }
}

android {
    namespace = "com.timothymarias.cookingapp.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}