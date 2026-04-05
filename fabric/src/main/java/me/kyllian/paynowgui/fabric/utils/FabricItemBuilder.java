package me.kyllian.paynowgui.fabric.utils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder for constructing Minecraft ItemStack instances on Fabric,
 * mirroring the Bukkit ItemBuilder API.
 */
public class FabricItemBuilder {

    private final ItemStack stack;

    public FabricItemBuilder(Item item) {
        this(item, 1);
    }

    public FabricItemBuilder(Item item, int count) {
        this.stack = new ItemStack(item, count);
    }

    public FabricItemBuilder(String materialName) {
        this(MaterialMapper.fromString(materialName), 1);
    }

    public FabricItemBuilder(String materialName, int count) {
        this(MaterialMapper.fromString(materialName), count);
    }

    public FabricItemBuilder setName(String name) {
        if (name == null || name.isEmpty()) return this;
        stack.set(DataComponentTypes.CUSTOM_NAME, FabricColorTranslator.toText(name));
        return this;
    }

    public FabricItemBuilder setLore(String lore) {
        if (lore == null || lore.isEmpty()) return this;

        String[] lines = lore.split("\\r?\\n");
        List<Text> loreLines = new ArrayList<>();
        for (String line : lines) {
            loreLines.add(FabricColorTranslator.toText(line));
        }

        stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        return this;
    }

    public FabricItemBuilder setCustomModelData(int data) {
        // In 1.21.4, custom model data is handled via DataComponentTypes
        // Skip if 0 (default / no override)
        if (data == 0) return this;
        // CustomModelData component was changed in 1.21.4+
        // For now, we skip custom model data on Fabric since it requires
        // the exact component type which may vary by MC version
        return this;
    }

    public FabricItemBuilder setEnchanted(boolean enchanted) {
        if (!enchanted) return this;
        // Add enchantment glint without a real enchantment
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return this;
    }

    public ItemStack toItemStack() {
        return stack;
    }
}
