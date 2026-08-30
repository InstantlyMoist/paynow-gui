pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    // Picks the right Loom variant per version: MC 26.1+ ships unobfuscated.
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        // Every stable Minecraft release with the item-component API (1.20.5+).
        match("1.20.5",  "fabric")
        match("1.20.6",  "fabric", "neoforge")
        match("1.21",    "fabric", "neoforge")
        match("1.21.1",  "fabric", "neoforge")
        match("1.21.2",  "fabric")
        match("1.21.3",  "fabric", "neoforge")
        match("1.21.4",  "fabric", "neoforge")
        match("1.21.5",  "fabric", "neoforge")
        match("1.21.6",  "fabric")
        match("1.21.7",  "fabric")
        match("1.21.8",  "fabric", "neoforge")
        match("1.21.9",  "fabric")
        match("1.21.10", "fabric", "neoforge")
        match("1.21.11", "fabric", "neoforge")
        match("26.1",    "fabric")
        match("26.1.1",  "fabric")
        match("26.1.2",  "fabric", "neoforge")
        match("26.2",    "fabric", "neoforge")

        vcsVersion = "1.21.4-fabric"
    }
}

rootProject.name = "paynow-gui"

include("core")
include("bukkit")
