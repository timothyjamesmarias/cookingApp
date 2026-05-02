plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.compose.hot-reload")
}

kotlin {
    jvm("desktop") {
    }
    
    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(project(":frontend:shared"))
            implementation(compose.desktop.currentOs)
            implementation("io.insert-koin:koin-core:4.1.0")
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "CookingApp"
            packageVersion = "1.0.0"
        }
    }
}