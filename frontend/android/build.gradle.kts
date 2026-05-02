plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    
    sourceSets {
        val androidMain by getting
        androidMain.dependencies {
            implementation(project(":frontend:shared"))
            implementation("androidx.activity:activity-compose:1.10.1")
            implementation("io.insert-koin:koin-core:4.1.0")
        }
    }
}

android {
    namespace = "com.timothymarias.cookingapp.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.timothymarias.cookingapp.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
