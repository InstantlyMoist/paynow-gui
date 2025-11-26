package me.kyllian.PayNowGUI.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.kyllian.PayNowGUI.utils.StringUtils.colorize;

@Getter
public abstract class BasicInventory<P extends JavaPlugin> implements Listener, InventoryHolder {

    protected final P plugin;
    protected final HashMap<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    protected Inventory inventory;
    @NotNull
    protected final ConfigurationSection section;

    public BasicInventory(P plugin, FileConfiguration root, String path) {
        this.plugin = plugin;

        section = Objects.requireNonNull(root.getConfigurationSection(path));

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getCurrentItem() == null || event
                .getCurrentItem().getType() == Material.AIR)
            return;

        if (event.getClickedInventory().getHolder() != this) {
            onBottomClick(event);
            return;
        }
        onTopClick(event);
        if (!actions.containsKey(event.getSlot())) return;
        Consumer<InventoryClickEvent> action = actions.get(event.getSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    public void addItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void addItem(List<Integer> slots, ItemStack item) {
        for (Integer slot : slots) {
            inventory.setItem(slot, item);
        }
    }

    public void addItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    public void addItem(List<Integer> slots, ItemStack item, Consumer<InventoryClickEvent> action) {
        for (Integer slot : slots) {
            inventory.setItem(slot, item);
            actions.put(slot, action);
        }
    }

    public void onClose(InventoryCloseEvent event) {

    }

    public void onTopClick(InventoryClickEvent event) {}
    public void onBottomClick(InventoryClickEvent event) {}

    @EventHandler
    public void onCloseEvent(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) return;
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
        this.inventory = null;
        onClose(event);
    }

    public int getSlots() {
        return section.getInt("size");
    }

    public List<Integer> getBackSlots() {
        return getSlots(section.getString("back_item.slots"));
    }

    public Inventory getInventory() {
        String type = section.getString("type");

        if (type == null || type.isEmpty())
            inventory = Bukkit.createInventory(this, getSlots(), colorize(titlePlaceholders(section.getString("title"))));
        else
            inventory = Bukkit.createInventory(this, InventoryType.valueOf(type), colorize(titlePlaceholders(section.getString("title"))));

        // If the section has back_item, handle it
        if (section.contains("back_item")) {
            ItemStack backItem = new ItemBuilder(Material.valueOf(section.getString("back_item.material")))
                    .setName(section.getString("back_item.name"))
                    .toItemStack();

            addItem(getBackSlots(), backItem, (event) -> onBack((Player) event.getWhoClicked()));
        }

        this.buildInventory();
        return inventory;
    }

    public String titlePlaceholders(String title) {
        return title;
    }

    public List<Integer> getSlots(String slots) {
        List<String> slotList = List.of(slots.split(","));
        List<Integer> finalSlots = new ArrayList<>();
        for (String slot : slotList) {
            if (slot.contains("-")) {
                // Handle rang
                String[] parts = slot.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                finalSlots.addAll(IntStream.rangeClosed(start, end)
                        .boxed()
                        .collect(Collectors.toList()));
            } else {
                // Single slot
                finalSlots.add(Integer.parseInt(slot));
            }
        }
        return finalSlots;
    }

    public void nextPage() {
        ItemStack nextPageItem = new ItemBuilder(Material.valueOf(section.getString("next_item.item")))
                .setName(section.getString("next_item.name")).toItemStack();

        addItem(section.getInt("next_item.slot"), nextPageItem, (event) -> onNextPage((Player) event.getWhoClicked()));
    }

    public void previousPage() {
        ItemStack previousPageItem = new ItemBuilder(Material.valueOf(section.getString("previous_item.item")))
                .setName(section.getString("previous_item.name")).toItemStack();

        addItem(section.getInt("previous_item.slot"), previousPageItem, (event) -> onPreviousPage((Player) event.getWhoClicked()));
    }

    public boolean hasNextPage(int currentPage, int slots, int totalSlots) {
        int maxPage = (int) Math.ceil((double) totalSlots / slots);
        return currentPage < maxPage;
    }

    public abstract void buildInventory();

    public void onNextPage(Player player) {
    }

    public void onPreviousPage(Player player) {
    }

    public void onBack(Player player) {
    }
}