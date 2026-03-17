plugins {
    id("java")
    id("io.freefair.lombok") version "9.2.0"
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.atownyserver.com/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    implementation(project(":auxprotect-core"))
    implementation("dev.kshl", "KshLib", "2.0")
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    implementation("jakarta.annotation", "jakarta.annotation-api", "3.0.0")
}