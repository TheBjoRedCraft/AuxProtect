plugins {
    kotlin("jvm") version "2.4.0" apply false
}

allprojects {
    group = "dev.heliosares.auxprotect"
    version = findProperty("version") as String
}