import java.io.ByteArrayOutputStream
import java.util.Locale.getDefault

plugins {
    `java-library`
    java
    application
    id("maven-publish")
    alias(libs.plugins.kotlinJvm)
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
    mainClass.set("${group}.nekomsl.MainKt")
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
    implementation(libs.gson)
    implementation(libs.slf4j)
    implementation(libs.sysoutOverSlf4j)
    implementation(libs.bundles.logback)
    implementation(libs.jetbrains.annotations)
    implementation(libs.commons.io)
    implementation(libs.commons.codec)
    implementation(libs.hutool)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.bundles.kotlinScriping)
}

subprojects{
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

tasks.getByName("processResources") {
    dependsOn("generateProperties")
}

task("generateProperties") {
    doLast {
        generateProperties()
    }
}

fun getGitBranch(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "symbolic-ref", "--short", "-q", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString(Charsets.UTF_8).trim()
}

fun getCommitId(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString(Charsets.UTF_8).trim()
}

fun generateProperties() {
    val propertiesFile = file("./src/main/resources/build.properties")
    if (propertiesFile.exists()) {
        propertiesFile.delete()
    }
    propertiesFile.createNewFile()
    val m = mutableMapOf<String, String>()
    propertiesFile.printWriter().use { writer ->
        properties.forEach {
            val str = it.value.toString()
            if ("@" in str || "(" in str || ")" in str || "extension" in str || "null" == str || "\'" in str || "\\" in str || "/" in str) return@forEach
            if ("PROJECT" in str.uppercase(getDefault()) || "PROJECT" in it.key.uppercase(getDefault()) || " " in str) return@forEach
            if ("GRADLE" in it.key.uppercase(getDefault()) || "GRADLE" in str.uppercase(getDefault()) || "PROP" in it.key.uppercase(getDefault())) return@forEach
            if ("." in it.key || "TEST" in it.key.uppercase(getDefault())) return@forEach
            if (it.value.toString().length <= 2) return@forEach
            m += it.key to str
        }
        m += "buildTime" to System.currentTimeMillis().toString()
        m += "branch" to getGitBranch()
        m += "commitId" to getCommitId()
        m.toSortedMap().forEach {
            writer.println("${it.key} = ${it.value}")
        }
    }
}