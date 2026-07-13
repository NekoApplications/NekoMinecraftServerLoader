plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinCompose)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4:${libs.versions.compose.get()}")
}

tasks.test {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
