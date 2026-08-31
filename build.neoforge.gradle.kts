plugins {
    id("net.neoforged.moddev") version "2.0.144"
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-neoforge"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// Runtime deps that must ship inside the mod jar (Minecraft already provides gson).
val bundle: Configuration by configurations.creating
configurations.named("implementation").get().extendsFrom(bundle)

val bundleAllow = listOf("core", "java-sdk", "gson-fire", "okhttp", "okio", "kotlin-stdlib", "snakeyaml")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
}

dependencies {
    // The SDK serialises with gson; its jackson transitives clash with ModDevGradle's
    // own jackson constraint and are never shipped, so drop them here.
    bundle(project(":core")) {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "org.openapitools", module = "jackson-databind-nullable")
    }
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

neoForge {
    version = sc.properties.get<String>("deps.neo_loader")

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain { languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion) }
}

val modProps = mapOf(
    "mod_id" to sc.properties.get<String>("mod.id"),
    "mod_name" to sc.properties.get<String>("mod.name"),
    "mod_version" to project.version.toString(),
    "mod_mc_compat" to sc.properties.get<String>("mod.mc_compat"),
)

tasks.processResources {
    inputs.properties(modProps)
    filesMatching("META-INF/neoforge.mods.toml") { expand(modProps) }
    // fabric.mod.json is meaningless in a NeoForge jar
    exclude("fabric.mod.json")
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Configuration.elements carries task dependencies, so :core:jar is built first.
    from(bundle.elements.map { locations ->
        locations.map { it.asFile }
            .filter { f -> bundleAllow.any { f.name.startsWith(it) } }
            .map { if (it.isDirectory) it else zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF")
        exclude("META-INF/maven/**", "module-info.class")
    }
}
