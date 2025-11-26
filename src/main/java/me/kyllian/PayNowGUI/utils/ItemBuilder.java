package me.kyllian.PayNowGUI.utils;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ItemBuilder {

    private final ItemStack itemStack;

    public ItemBuilder(Material m) {
        this(m, 1);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    public ItemBuilder(Material m, int amount) {
        this.itemStack = new ItemStack(m, amount);
    }

    public ItemBuilder(Material m, int amount, byte durability) {
        this.itemStack = new ItemStack(m, amount, durability);
    }

    public ItemBuilder clone() {
        return new ItemBuilder(this.itemStack);
    }

    public ItemBuilder setDurability(short dur) {
        this.itemStack.setDurability(dur);
        return this;
    }

    public ItemBuilder setName(String name) {
        name = StringUtils.colorize(name);
        ItemMeta im = this.itemStack.getItemMeta();
        im.setDisplayName(name);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder addUnsafeEnchantment(Enchantment ench, int level) {
        this.itemStack.addUnsafeEnchantment(ench, level);
        return this;
    }

    public ItemBuilder removeEnchantment(Enchantment ench) {
        this.itemStack.removeEnchantment(ench);
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        try {
            SkullMeta im = (SkullMeta) this.itemStack.getItemMeta();
            im.setOwner(owner);
            this.itemStack.setItemMeta(im);
        } catch (ClassCastException classCastException) {
        }
        return this;
    }

    public ItemBuilder setSkullOwner(OfflinePlayer owningPlayer) {
        try {
            SkullMeta im = (SkullMeta) this.itemStack.getItemMeta();
            im.setOwningPlayer(owningPlayer);
            this.itemStack.setItemMeta(im);
        } catch (ClassCastException classCastException) {
        }
        return this;
    }

    public ItemBuilder addEnchant(Enchantment ench, int level) {
        ItemMeta im = this.itemStack.getItemMeta();
        im.addEnchant(ench, level, true);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments) {
        this.itemStack.addEnchantments(enchantments);
        return this;
    }

    public ItemBuilder setEnchanted(boolean enchanted) {
        if (!enchanted) return this;
        ItemMeta im = this.itemStack.getItemMeta();
        im.addEnchant(Enchantment.UNBREAKING, 1, true);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder setInfinityDurability() {
        ItemMeta im = this.itemStack.getItemMeta();
        im.setUnbreakable(true);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder setLore(String lore) {
        if (lore == null || lore.isEmpty()) return this;
        lore = StringUtils.colorize(lore);
        ItemMeta im = this.itemStack.getItemMeta();
        ArrayList<String> arrayList = new ArrayList<>();
        String[] str = lore.split("\\r?\\n");
        if (str[0] == null) {
            arrayList.add(lore);
        } else {
            arrayList.addAll(Arrays.asList(str));
        }
        im.setLore(arrayList);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... itemFlag) {
        ItemMeta im = this.itemStack.getItemMeta();
        im.addItemFlags(itemFlag);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemBuilder removeItemFlags(ItemFlag... itemFlag) {
        ItemMeta im = this.itemStack.getItemMeta();
        im.removeItemFlags(itemFlag);
        this.itemStack.setItemMeta(im);
        return this;
    }

    public ItemStack toItemStack() {
        return this.itemStack;
    }
}