plugins {
    id("java-library")
}

group = "me.kyllian"
version = "1.2.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

dependencies {
    api("gg.paynow:java-sdk:1.0.72-java8")
    api("org.yaml:snakeyaml:2.3")
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.squareup.okhttp3:logging-interceptor:5.2.1")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    // Java 8 bytecode so the Bukkit jar loads on 1.8 servers. Modern loaders read it fine.
    options.release = 8
}
