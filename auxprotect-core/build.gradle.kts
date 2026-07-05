plugins {
    id("java")
    id("io.freefair.lombok") version "9.2.0"
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven("https://jitpack.io")
    maven("https://maven.atownyserver.com/") // For KshLib
}

val exposedVersion = "0.46.0"

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.51.3.0")
    implementation("org.json:json:20240303")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("org.yaml:snakeyaml:2.6")
    implementation("dev.kshl:KshLib:2.0")
    implementation("net.kyori:adventure-api:5.2.0")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // HikariCP connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")
}