import org.gradle.api.tasks.WriteProperties
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `java-library`
    java
    application
    id("maven-publish")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
}

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

application {
    mainClass.set("${group}.nekomsl.DirectMainKt")
}

description = "neko-minecraft-server-loader"
java.sourceCompatibility = JavaVersion.VERSION_25

kotlin {
    jvmToolchain(25)
}

tasks {
    shadowJar {
        archiveClassifier.set("full")
    }
}

dependencies {
    implementation(libs.jline)
    implementation(libs.slf4j)
    implementation(libs.sysoutOverSlf4j)
    implementation(libs.bundles.logback)
    implementation(libs.jetbrains.annotations)
    implementation(libs.commons.io)
    implementation(libs.commons.codec)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.argparser)
    implementation(libs.bundles.kotlinScriping)
}

fun gitValue(vararg args: String) = providers.provider {
    runCatching {
        providers.exec {
            workingDir(rootProject.projectDir)
            commandLine("git", *args)
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim()
    }.getOrDefault("")
        .takeUnless { it.isEmpty() || it == "HEAD" }
        ?: "unknown"
}

val generateBuildProperties = tasks.register<WriteProperties>("generateBuildProperties") {
    destinationFile.set(
        layout.buildDirectory.file("generated/resources/buildProperties/build.properties")
    )
    encoding = "UTF-8"
    lineSeparator = "\n"

    property("version", project.version.toString())
    property("branch", gitValue("rev-parse", "--abbrev-ref", "HEAD"))
    property("commitId", gitValue("rev-parse", "HEAD"))
    property("buildTime", providers.provider { System.currentTimeMillis().toString() })

    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("Contains the current build time") { true }

    doFirst {
        destinationFile.get().asFile.parentFile.mkdirs()
    }
}

tasks.named<ProcessResources>("processResources") {
    from(generateBuildProperties)
}
