plugins {
    id("java")
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
    id("io.freefair.lombok") version "9.2.0"
    id("com.gradleup.shadow") version "9.4.0"
    kotlin("jvm")
}

paper {
    main = "dev.heliosares.auxprotect.AuxProtectPaper"
    authors = listOf("Heliosares", "ks-hl", "red")
    name = "AuxProtect"
    apiVersion = "1.21.11"
    description = "A plugin designed to supplement CoreProtect in a few ways."
    foliaSupported = true

    serverDependencies {
        register("CoreProtect") {
            required = false
        }
    }
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.minebench.de/")
    maven("https://jitpack.io")
    maven("https://maven.playpro.com")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.olziedev.com/")
    maven("https://repo.nightexpressdev.com/releases")
    maven("https://maven.atownyserver.com/")  // For KshLib
    maven("https://mvn-repo.arim.space/lesser-gpl3/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.48-alpha")

    implementation(project(":auxprotect-core"))
    implementation("dev.kshl:KshLib:2.0")

    compileOnly("net.coreprotect:coreprotect:22.4")
    implementation("io.papermc:paperlib:1.0.7")
    implementation("space.arim.morepaperlib:morepaperlib:0.5.4-SNAPSHOT")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}

tasks.shadowJar {
    relocate("io.papermc:paperlib", "dev.heliosares.auxprotect.lib.paperlib")
    relocate("space.arim.morepaperlib", "dev.heliosares.auxprotect.lib.morepaperlib")
}