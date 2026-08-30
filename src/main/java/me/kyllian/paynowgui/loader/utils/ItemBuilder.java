package me.kyllian.paynowgui.loader.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder for constructing Minecraft ItemStack instances on Fabric,
 * mirroring the Bukkit ItemBuilder API.
 */
public class ItemBuilder {

    private final ItemStack stack;

    public ItemBuilder(Item item) {
        this(item, 1);
    }

    public ItemBuilder(Item item, int count) {
        this.stack = new ItemStack(item, count);
    }

    public ItemBuilder(String materialName) {
        this(MaterialMapper.fromString(materialName), 1);
    }

    public ItemBuilder(String materialName, int count) {
        this(MaterialMapper.fromString(materialName), count);
    }

    public ItemBuilder setName(String name) {
        if (name == null || name.isEmpty()) return this;
        stack.set(DataComponents.CUSTOM_NAME, ColorTranslator.toText(name));
        return this;
    }

    public ItemBuilder setLore(String lore) {
        if (lore == null || lore.isEmpty()) return this;

        String[] lines = lore.split("\\r?\\n");
        List<Component> loreLines = new ArrayList<>();
        for (String line : lines) {
            loreLines.add(ColorTranslator.toText(line));
        }

        stack.set(DataComponents.LORE, new ItemLore(loreLines));
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        if (data == 0) return this;
        // CustomModelData became a four-list component in 1.21.4; a plain int before.
        //? if >=1.21.4 {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of((float) data), List.of(), List.of(), List.of()));
        //?} else {
        /*stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(data));
        *///?}
        return this;
    }

    public ItemBuilder setEnchanted(boolean enchanted) {
        if (!enchanted) return this;
        // Add enchantment glint without a real enchantment
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return this;
    }

    public ItemStack toItemStack() {
        return stack;
    }
}
