plugins {
    id("java")
    id("io.freefair.lombok") version "9.2.0"
}

repositories {
    mavenCentral()
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://maven.atownyserver.com/") }
}

dependencies {
    implementation("org.xerial", "sqlite-jdbc", "3.51.3.0")
    implementation("org.json", "json", "20240303")
    implementation("jakarta.annotation", "jakarta.annotation-api", "3.0.0")
    implementation("org.yaml", "snakeyaml", "2.2")
    implementation("dev.kshl", "KshLib", "2.0")
}