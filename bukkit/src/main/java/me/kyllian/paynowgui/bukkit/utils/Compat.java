package me.kyllian.paynowgui.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Bridges the Bukkit API differences between 1.8 and current releases so a single
 * jar can run across the whole range. Everything here resolves reflectively or by
 * name, so it never hard-links a symbol that is absent on the running server.
 */
public final class Compat {

    private Compat() {}

    /** Pre-1.13 ("flattening") names for materials whose id changed. */
    private static final Map<String, String> LEGACY_ALIASES = new HashMap<>();

    static {
        LEGACY_ALIASES.put("CHEST_MINECART", "STORAGE_MINECART");
        LEGACY_ALIASES.put("COMMAND_BLOCK_MINECART", "COMMAND_MINECART");
        LEGACY_ALIASES.put("TNT_MINECART", "EXPLOSIVE_MINECART");
        LEGACY_ALIASES.put("FURNACE_MINECART", "POWERED_MINECART");
        LEGACY_ALIASES.put("HOPPER_MINECART", "HOPPER_MINECART");
        LEGACY_ALIASES.put("CRAFTING_TABLE", "WORKBENCH");
        LEGACY_ALIASES.put("ENCHANTING_TABLE", "ENCHANTMENT_TABLE");
        LEGACY_ALIASES.put("PLAYER_HEAD", "SKULL_ITEM");
        LEGACY_ALIASES.put("OAK_SIGN", "SIGN");
        LEGACY_ALIASES.put("SPAWNER", "MOB_SPAWNER");
        LEGACY_ALIASES.put("GUNPOWDER", "SULPHUR");
        LEGACY_ALIASES.put("NETHER_BRICKS", "NETHER_BRICK");
        LEGACY_ALIASES.put("CLOCK", "WATCH");
        LEGACY_ALIASES.put("EXPERIENCE_BOTTLE", "EXP_BOTTLE");
        LEGACY_ALIASES.put("FIREWORK_ROCKET", "FIREWORK");
        LEGACY_ALIASES.put("COBWEB", "WEB");
        LEGACY_ALIASES.put("TERRACOTTA", "HARD_CLAY");
        LEGACY_ALIASES.put("GOLDEN_SWORD", "GOLD_SWORD");
        LEGACY_ALIASES.put("GOLDEN_APPLE", "GOLDEN_APPLE");
        LEGACY_ALIASES.put("WHEAT_SEEDS", "SEEDS");
        LEGACY_ALIASES.put("LILY_PAD", "WATER_LILY");
    }

    /**
     * Resolve a configured material name on any server version, falling back to a
     * pre-flattening alias and finally to STONE rather than throwing.
     */
    public static Material material(String name) {
        if (name == null || name.isEmpty()) return Material.STONE;
        String upper = name.toUpperCase();

        Material direct = Material.matchMaterial(upper);
        if (direct != null) return direct;

        String alias = LEGACY_ALIASES.get(upper);
        if (alias != null) {
            Material legacy = Material.matchMaterial(alias);
            if (legacy != null) return legacy;
        }

        Bukkit.getLogger().warning("[paynow-gui] Unknown material '" + name
                + "' on this server version; falling back to STONE.");
        return Material.STONE;
    }

    /**
     * The enchantment used purely to give an item a glint. Named UNBREAKING from
     * 1.20.5 and DURABILITY before it.
     */
    public static Enchantment glintEnchantment() {
        for (String field : new String[]{"UNBREAKING", "DURABILITY"}) {
            try {
                Field f = Enchantment.class.getField(field);
                Object value = f.get(null);
                if (value instanceof Enchantment) return (Enchantment) value;
            } catch (ReflectiveOperationException ignored) {
                // try the next name
            }
        }
        return null;
    }

    /** ItemMeta#setCustomModelData only exists from 1.14. */
    public static void setCustomModelData(ItemMeta meta, int data) {
        if (meta == null || data == 0) return;
        try {
            Method m = meta.getClass().getMethod("setCustomModelData", Integer.class);
            m.invoke(meta, data);
        } catch (ReflectiveOperationException ignored) {
            // pre-1.14 server: custom model data is not supported, skip silently
        }
    }
}
