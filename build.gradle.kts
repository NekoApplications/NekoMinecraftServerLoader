plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.shadow) apply false
}

subprojects {
    group = "icu.takeneko"
    version = rootProject.property("version")!!

    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
