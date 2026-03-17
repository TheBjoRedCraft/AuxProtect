import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java")
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
    id("io.freefair.lombok") version "9.2.0"
}

paper {
    main = "dev.heliosares.auxprotect.spigot.AuxProtectSpigot"
    authors = listOf("Heliosares", "ks-hl", "red")
    name = "AuxProtect"
    apiVersion = "1.21.11"
    description = "A plugin designed to supplement CoreProtect in a few ways."
    foliaSupported = true

    serverDependencies {
        register("AuctionHouse") {
            required = false
        }
        register("ChestShop") {
            required = false
        }
        register("CoreProtect") {
            required = false
        }
        register("DynamicShop") {
            required = false
        }
        register("EconomyShopGUI") {
            required = false
        }
        register("EconomyShopGUI-Premium") {
            required = false
        }
        register("Essentials") {
            required = false
        }
        register("PlayerAuctions") {
            required = false
        }
        register("ProtocolLib") {
            required = false
        }
        register("Jobs") {
            required = false
        }
        register("ShopGUIPlus") {
            required = false
        }
        register("Towny") {
            required = false
        }
        register("Vault") {
            required = false
        }
        register("AdvancedPortals") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("AnnouncerPlus") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("ajParkour") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven("https://repo.minebench.de/")
    maven("https://jitpack.io")
    maven("https://maven.playpro.com")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.glaremasters.me/repository/towny/")
    maven("https://repo.olziedev.com/")
    maven("https://repo.nightexpressdev.com/releases")
    maven { url = uri("https://maven.atownyserver.com/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    implementation(project(":auxprotect-core"))
    implementation("dev.kshl", "KshLib", "2.0")

    compileOnly("com.acrobot.chestshop:chestshop:3.12.2")
    compileOnly("net.coreprotect:coreprotect:22.4")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    implementation("com.github.brcdev-minecraft:shopgui-api:3.0.0") {
        isTransitive = false
    }
    implementation("com.github.Gypopo:EconomyShopGUI-API:1.7.1")

    compileOnly("com.github.Zrips:Jobs:4.17.2") {
        isTransitive = false
    }
    compileOnly("net.essentialsx:EssentialsX:2.20.1")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.2.0")

    compileOnly("com.github.Heliosares:AuctionHouseAPI:756e099dff")
    compileOnly("com.olziedev:playerauctions-api:1.27.3")

    implementation("su.nightexpress.excellentcrates:ExcellentCrates:6.5.0")
    implementation("su.nightexpress.nightcore:main:2.10.0")
}