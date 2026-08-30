plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.4-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties { tags(version, loader) }
    constants { match(loader, "fabric", "neoforge") }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    dependencies["fapi"] = properties.getOrNull<String>("deps.fabric_api") ?: "0"

    replacements {
        // Mojang renamed ResourceLocation -> Identifier in 1.21.11.
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
        // ClickType -> ContainerInput in 26.1 (same enum constants).
        string(current.parsed >= "26.1") {
            replace("ClickType", "ContainerInput")
        }
    }
}
