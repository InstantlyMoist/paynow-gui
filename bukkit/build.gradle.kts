plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
}

group = "me.kyllian"
version = "1.2.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.lunarclient.dev") { name = "lunarclient" }
    maven("https://maven.citizensnpcs.co/repo") { name = "citizens" }
    // SpaceNPC/EntityLib live here; JitPack 401s for this group so pin it exclusively.
    exclusiveContent {
        forRepository { maven("https://maven.pvphub.me/tofaa") { name = "pvphub" } }
        filter { includeGroup("io.github.tofaa2") }
    }
    maven("https://repo.codemc.io/repository/maven-releases/") { name = "codemc" }
}

dependencies {
    implementation(project(":core"))

    compileOnly("org.spigotmc:spigot-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.lunarclient:apollo-api:1.2.4")
    compileOnly("com.lunarclient:apollo-extra-adventure4:1.2.4")
    compileOnly("net.citizensnpcs:citizens-main:2.0.37-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-api:2.13.0")
    compileOnly("net.kyori:adventure-api:4.25.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.25.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.25.0")

    compileOnly("io.github.tofaa2:spaceNPC:3.3.6-SNAPSHOT")
    compileOnly("io.github.tofaa2:api:3.3.6-SNAPSHOT")

    implementation("org.bstats:bstats-bukkit:3.1.0")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}


java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
    // spigot-api publishes no Gradle metadata, but be explicit that we target an older JVM.
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    // One jar for 1.8 -> 26.2, so emit Java 8 bytecode.
    options.release = 8
}

bukkit {
    main = "me.kyllian.paynowgui.bukkit.PayNowGUIPlugin"
    name = "paynow-gui"
    apiVersion = "1.13"
    authors = listOf("Kyllian")
    description = "Unofficial Spigot plugin for the PayNow API"
    softDepend = listOf("Apollo-Bukkit", "Citizens", "SpaceNPC")
    commands { register("buy") { usage = "/<command>" } }
}

tasks.shadowJar {
    from(rootProject.file("LICENSE"))
    archiveBaseName = "paynow-gui-bukkit"
    archiveClassifier = ""
    archiveVersion = ""

    // Spigot already provides gson; only ship what the server does not.
    dependencies {
        include(project(":core"))
        include(dependency("gg.paynow:java-sdk:.*"))
        include(dependency("io.gsonfire:gson-fire:.*"))
        include(dependency("org.bstats:.*"))
        include(dependency("com.squareup.okhttp3:.*"))
        include(dependency("com.squareup.okio:.*"))
        include(dependency("org.jetbrains.kotlin:kotlin-stdlib:.*"))
        include(dependency("org.yaml:snakeyaml:.*"))
    }

    relocate("gg.paynow", "me.kyllian.shaded.paynow")
    relocate("org.bstats", "me.kyllian.shaded.bstats")
    relocate("okhttp3", "me.kyllian.shaded.okhttp3")
    relocate("okio", "me.kyllian.shaded.okio")

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/maven/**")
}

tasks.named<Jar>("jar") { enabled = false }
