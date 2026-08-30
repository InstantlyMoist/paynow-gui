# Building paynow-gui

The repo produces three kinds of artifact from one shared codebase:

| Target | Versions | Output |
|---|---|---|
| Bukkit / Spigot / Paper | 1.8 → 26.2 (one jar) | `bukkit/build/libs/paynow-gui-bukkit.jar` |
| Fabric | 1.20.5 → 26.2 (18 jars) | `versions/<mc>-fabric/build/libs/` |
| NeoForge | 1.20.6 → 26.2 (11 jars) | `versions/<mc>-neoforge/build/libs/` |

## Prerequisites

* **JDK 21** — required for every target up to Minecraft 1.21.11.
* **JDK 25** — required to build the Minecraft 26.x nodes.
* **Maven** — only for the one-time SDK bootstrap below.

Register both JDKs with Gradle (already set in `gradle.properties`; adjust the paths
if yours differ):

```properties
org.gradle.java.installations.paths=/path/to/jdk-21,/path/to/jdk-25
```

## One-time: build the PayNow SDK

`gg.paynow:java-sdk` is **not published to any public repository**. It must be built
locally into `~/.m2` before anything else will resolve.

The SDK generates its sources from the live PayNow OpenAPI spec at build time, and its
preprocess script downgrades the spec from OpenAPI 3.1 to 3.0 — without that step,
openapi-generator 7.2.0 emits `java.lang.Object` for most fields and the generated
getters no longer match this project. The published JitPack builds
(`com.github.InstantlyMoist:paynow-gg-java-sdk`) have this problem, so they cannot be
substituted.

The Bukkit jar targets Java 8 bytecode so it can load on 1.8 servers, which means the
SDK must be built for Java 8 too:

```bash
git clone --depth 1 --branch 1.0.72 https://github.com/InstantlyMoist/paynow-gg-java-sdk.git
cd paynow-gg-java-sdk

# retarget to Java 8 and publish under a distinct version
sed -i '' -e 's|<maven.compiler.source>11</maven.compiler.source>|<maven.compiler.source>8</maven.compiler.source>|' \
          -e 's|<maven.compiler.target>11</maven.compiler.target>|<maven.compiler.target>8</maven.compiler.target>|' \
          -e 's|<source>11</source>|<source>8</source>|' \
          -e 's|<target>11</target>|<target>8</target>|' \
          -e 's|<release>11</release>|<release>8</release>|' \
          -e 's|<version>1.0.72</version>|<version>1.0.72-java8</version>|' pom.xml

mvn clean install -DskipTests
```

Requires `jq` and network access (it downloads the live spec).

> Publishing `1.0.72-java8` to a real repository would remove this manual step for
> everyone else who clones the project.

## Building

```bash
./gradlew :bukkit:build                    # the Bukkit plugin
./gradlew ":1.21.4-fabric:build"           # one Fabric version
./gradlew ":26.2-neoforge:build"           # one NeoForge version
```

## Multi-version layout

Version-spanning code is managed by [Stonecutter](https://stonecutter.kikugie.dev/).
The single source tree lives in `src/main/java`; each entry in `settings.gradle.kts`
becomes a build node under `versions/`.

All loader targets use **Mojang mappings** (`loomx.applyMojangMappings()` on Fabric,
native on NeoForge), so one namespace covers the whole matrix.

Source is preprocessed with `//?` directives, e.g.:

```java
//? if >=1.21.5 {
new ClickEvent.OpenUrl(URI.create(url))
//?} else {
/*new ClickEvent(ClickEvent.Action.OPEN_URL, url)
*///?}
```

The checked-in source always reflects the **active** node. Before committing, run:

```bash
./gradlew "Reset active project"
```

Switch versions with `./gradlew "Set active project to 26.2-fabric"`.

### Known version boundaries

| Change | Version |
|---|---|
| `CustomModelData` int → four lists | 1.21.4 |
| `Registry.get` → `getValue` | 1.21.2 |
| `ClickEvent`/`HoverEvent` → sealed records | 1.21.5 |
| `ResourceLocation` → `Identifier` | 1.21.11 |
| `ClickType` → `ContainerInput`, int perms → `PermissionSet` | 26.1 |
| Java 21 → Java 25 | 26.1 |

## Optional Bukkit dependencies

Citizens and SpaceNPC are `compileOnly` NPC backends. Citizens moved to
`https://maven.citizensnpcs.co/repo` (the old `repo.citizensnpcs.co` host now redirects,
which Gradle will not follow). SpaceNPC resolves from `maven.pvphub.me/tofaa`; its
JitPack coordinate is private and returns 401.
