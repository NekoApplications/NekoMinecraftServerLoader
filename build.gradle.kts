import java.util.Locale

plugins {
    `java-library`
    java
    application
    id("maven-publish")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
}

group = "icu.takeneko"
version = project.property("version")!!

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

repositories {
    mavenCentral()
    mavenLocal()
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

subprojects{
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

tasks.named("processResources") {
    dependsOn("generateProperties")
}

tasks.register("generateProperties") {
    doLast {
        generateProperties()
    }
}

fun getGitBranch(): String {
    return providers.exec {
        commandLine("git", "symbolic-ref", "--short", "-q", "HEAD")
    }.standardOutput.asText.get().trim()
}

fun getCommitId(): String {
    return providers.exec {
        commandLine("git", "rev-parse", "HEAD")
    }.standardOutput.asText.get().trim()
}

fun generateProperties() {
    val propertiesFile = file("./src/main/resources/build.properties")
    if (propertiesFile.exists()) {
        propertiesFile.delete()
    }
    propertiesFile.createNewFile()
    val m = mutableMapOf<String, String>()
    propertiesFile.printWriter().use { writer ->
        providers.gradlePropertiesPrefixedBy("").get().forEach { (key, str) ->
            if ("@" in str || "(" in str || ")" in str || "extension" in str || "null" == str || "\'" in str || "\\" in str || "/" in str) return@forEach
            if ("PROJECT" in str.uppercase(Locale.ROOT) || "PROJECT" in key.uppercase(Locale.ROOT) || " " in str) return@forEach
            if ("GRADLE" in key.uppercase(Locale.ROOT) || "GRADLE" in str.uppercase(Locale.ROOT) || "PROP" in key.uppercase(Locale.ROOT)) return@forEach
            if ("JRELEASER" in key.uppercase(Locale.ROOT)) return@forEach
            if ("." in key || "TEST" in key.uppercase(Locale.ROOT)) return@forEach
            if (str.length <= 2) return@forEach
            m += key to str
        }
        m += "buildTime" to System.currentTimeMillis().toString()
        m += "branch" to getGitBranch()
        m += "commitId" to getCommitId()
        m.toSortedMap().forEach {
            writer.println("${it.key} = ${it.value}")
        }
    }
}
