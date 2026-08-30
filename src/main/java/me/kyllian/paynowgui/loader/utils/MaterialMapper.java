package me.kyllian.paynowgui.loader.utils;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

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
        // ResourceLocation constructors were replaced by static factories in 1.21.
        //? if >=1.21 {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", registryName);
        //?} else {
        /*ResourceLocation id = new ResourceLocation("minecraft", registryName);
        *///?}

        // Registry.get() started returning Optional<Holder.Reference<T>> in 1.21.2;
        // getValue() is the direct accessor from then on.
        //? if >=1.21.2 {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        //?} else {
        /*Item item = BuiltInRegistries.ITEM.get(id);
        *///?}
        // BuiltInRegistries.ITEM.get() returns Items.AIR for unknown IDs
        if (item == Items.AIR && !registryName.equals("air")) {
            return Items.STONE; // fallback
        }
        return item;
    }
}
