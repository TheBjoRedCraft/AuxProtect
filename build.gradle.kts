plugins {
    kotlin("jvm") version "1.9.25" apply false
}

allprojects {
    group = "dev.heliosares.auxprotect"
    version = findProperty("version") as String
}