package me.kyllian.paynowgui.fabric.utils;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Bukkit-style material name strings (e.g. "DIAMOND_SWORD") to Minecraft Item instances.
 * Uses the Minecraft registry as the primary lookup, with a manual fallback map for edge cases.
 */
public class MaterialMapper {

    private static final Map<String, Item> OVERRIDES = new HashMap<>();

    static {
        // Add any Bukkit names that differ from MC registry identifiers
        OVERRIDES.put("CHEST_MINECART", Items.CHEST_MINECART);
    }

    /**
     * Convert a Bukkit-style material name to a Minecraft Item.
     * E.g. "DIAMOND_SWORD" -> Items.DIAMOND_SWORD
     *
     * @param materialName The material name (case-insensitive, underscores preserved)
     * @return The matching Item, or Items.STONE as fallback
     */
    public static Item fromString(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Items.STONE;
        }

        String normalized = materialName.toUpperCase().trim();

        // Check overrides first
        Item override = OVERRIDES.get(normalized);
        if (override != null) return override;

        // Convert to MC registry format: DIAMOND_SWORD -> minecraft:diamond_sword
        String registryName = normalized.toLowerCase();
        Identifier id = Identifier.of("minecraft", registryName);

        Item item = Registries.ITEM.get(id);
        // Registries.ITEM.get() returns Items.AIR for unknown IDs
        if (item == Items.AIR && !registryName.equals("air")) {
            return Items.STONE; // fallback
        }
        return item;
    }
}
