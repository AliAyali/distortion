
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.aliayali.distortion.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidLibraryDesugaring") {
            id = "com.aliayali.distortion.android.library.desugaring"
            implementationClass =
                "com.aliayali.distortion.convention.AndroidLibraryDesugaringConventionPlugin"
        }

        register("androidApplication") {
            id = "com.aliayali.distortion.android.application"
            implementationClass =
                "com.aliayali.distortion.convention.AndroidApplicationConventionPlugin"
        }

        register("androidLibrary") {
            id = "com.aliayali.distortion.android.library"
            implementationClass =
                "com.aliayali.distortion.convention.AndroidLibraryConventionPlugin"
        }

        register("androidCompose") {
            id = "com.aliayali.distortion.android.compose"
            implementationClass =
                "com.aliayali.distortion.convention.AndroidComposeConventionPlugin"
        }

        register("androidLibraryCompose") {
            id = "com.aliayali.distortion.android.library.compose"
            implementationClass =
                "com.aliayali.distortion.convention.AndroidLibraryComposeConventionPlugin"
        }
    }
}