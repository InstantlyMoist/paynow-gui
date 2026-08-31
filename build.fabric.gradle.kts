plugins {
    id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// Runtime deps that must ship inside the mod jar. Minecraft already provides gson,
// so the SDK's heavier transitives (jackson/apache/joda/swagger) are left out.
val bundle: Configuration by configurations.creating
configurations.named("implementation").get().extendsFrom(bundle)

val bundleAllow = listOf("core", "java-sdk", "gson-fire", "okhttp", "okio", "kotlin-stdlib", "snakeyaml")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://maven.fabricmc.net/") { name = "FabricMC" }
}

dependencies {
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // One namespace across the whole matrix - and the same one NeoForge uses.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    fapi("fabric-lifecycle-events-v1", "fabric-command-api-v2")

    bundle(project(":core"))
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
}


val modProps = mapOf(
    "mod_id" to sc.properties["mod.id"],
    "mod_name" to sc.properties["mod.name"],
    "mod_version" to project.version.toString(),
    "mod_mc_compat" to sc.properties["mod.mc_compat"],
)

tasks.processResources {
    inputs.properties(modProps)
    filesMatching("fabric.mod.json") { expand(modProps) }
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
