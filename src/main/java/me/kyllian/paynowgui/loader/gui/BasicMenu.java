package me.kyllian.paynowgui.loader.gui;

import me.kyllian.paynowgui.core.utils.YMLFile;
import me.kyllian.paynowgui.loader.PayNowLoaderMod;
import me.kyllian.paynowgui.loader.utils.ColorTranslator;
import me.kyllian.paynowgui.loader.utils.ItemBuilder;
import me.kyllian.paynowgui.loader.utils.MaterialMapper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.*;


/**
 * Base class for server-side Fabric GUIs using chest-style container inventories.
 * <p>
 * Opens a ChestMenu backed by a SimpleContainer.
 * Slot click actions are tracked via a map, similar to Bukkit's BasicInventory.
 */
public abstract class BasicMenu {

    protected final PayNowLoaderMod mod;
    protected final YMLFile config;
    protected final String sectionPath;

    protected SimpleContainer inventory;
    protected final Map<Integer, ClickHandler> actions = new HashMap<>();

    /**
     * Receives the raw mouse button alongside the action type, so left-click and
     * right-click can be told apart (ClickType alone cannot distinguish them).
     * button 0 = left, 1 = right; QUICK_MOVE means shift was held.
     */
    @FunctionalInterface
    public interface ClickHandler {
        void onClick(ServerPlayer player, int button, ClickType actionType);
    }

    private boolean closed = false;

    public BasicMenu(PayNowLoaderMod mod, String sectionPath) {
        this.mod = mod;
        this.config = mod.getPlatform().getConfig();
        this.sectionPath = sectionPath;
    }

    // ---- Config helpers using dot-path scoped to this section ----

    protected String cfgString(String subPath) {
        return config.getString(sectionPath + "." + subPath);
    }

    protected String cfgString(String subPath, String def) {
        return config.getString(sectionPath + "." + subPath, def);
    }

    protected int cfgInt(String subPath) {
        return config.getInt(sectionPath + "." + subPath);
    }

    protected int cfgInt(String subPath, int def) {
        return config.getInt(sectionPath + "." + subPath, def);
    }

    protected boolean cfgBool(String subPath) {
        return config.getBoolean(sectionPath + "." + subPath);
    }

    @SuppressWarnings("unchecked")
    protected List<Integer> cfgIntList(String subPath) {
        Object val = config.get(sectionPath + "." + subPath);
        if (val instanceof List<?> list) {
            List<Integer> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number n) result.add(n.intValue());
            }
            return result;
        }
        return Collections.emptyList();
    }

    protected List<String> cfgStringList(String subPath) {
        return config.getStringList(sectionPath + "." + subPath);
    }

    protected boolean cfgHas(String subPath) {
        return config.has(sectionPath + "." + subPath);
    }

    // ---- Inventory management ----

    public int getSlotCount() {
        return cfgInt("size", 27);
    }

    public String titlePlaceholders(String title) {
        return title;
    }

    /**
     * Build the inventory contents. Subclasses populate the inventory here.
     */
    public abstract void buildInventory();

    /**
     * Open the GUI for the given player.
     */
    public void open(ServerPlayer player) {
        int slots = getSlotCount();
        // Clamp to valid chest row counts (9, 18, 27, 36, 45, 54)
        int rows = Math.max(1, Math.min(6, (slots + 8) / 9));
        int totalSlots = rows * 9;

        inventory = new SimpleContainer(totalSlots);
        buildInventory();

        String rawTitle = cfgString("title", "GUI");
        Component title = ColorTranslator.toText(titlePlaceholders(rawTitle));

        MenuType<ChestMenu> handlerType = getHandlerType(rows);

        player.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, p) -> {
                    ChestMenu handler = new ChestMenu(handlerType, syncId, playerInventory, inventory, rows) {
                        @Override
                        public void clicked(int slotIndex, int button, ClickType actionType, net.minecraft.world.entity.player.Player clickPlayer) {
                            // Prevent taking items
                            if (slotIndex >= 0 && slotIndex < totalSlots) {
                                ClickHandler action = actions.get(slotIndex);
                                if (action != null && clickPlayer instanceof ServerPlayer spe) {
                                    action.onClick(spe, button, actionType);
                                }
                                return; // Cancel the click
                            }
                            // Also cancel clicks on player inventory
                        }

                        @Override
                        public void removed(net.minecraft.world.entity.player.Player closingPlayer) {
                            super.removed(closingPlayer);
                            if (!closed) {
                                closed = true;
                                BasicMenu.this.onClose(player);
                            }
                        }
                    };
                    return handler;
                },
                title
        ));
    }

    protected void addItem(int slot, ItemStack item) {
        if (inventory != null && slot >= 0 && slot < inventory.getContainerSize()) {
            inventory.setItem(slot, item);
        }
    }

    protected void addItem(int slot, ItemStack item, ClickHandler action) {
        addItem(slot, item);
        actions.put(slot, action);
    }

    protected void addItem(List<Integer> slots, ItemStack item) {
        for (int slot : slots) {
            addItem(slot, item);
        }
    }

    protected void addItem(List<Integer> slots, ItemStack item, ClickHandler action) {
        for (int slot : slots) {
            addItem(slot, item);
            actions.put(slot, action);
        }
    }

    /**
     * Called when the inventory is closed.
     */
    protected void onClose(ServerPlayer player) {
        // Override in subclasses
    }

    protected boolean isClosed() {
        return closed;
    }

    protected void setClosed(boolean closed) {
        this.closed = closed;
    }

    // ---- Utility ----

    protected List<Integer> parseSlots(String slotsString) {
        if (slotsString == null || slotsString.isEmpty()) return List.of();
        List<Integer> result = new ArrayList<>();
        for (String part : slotsString.split(",")) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = start; i <= end; i++) result.add(i);
            } else {
                result.add(Integer.parseInt(part));
            }
        }
        return result;
    }

    private MenuType<ChestMenu> getHandlerType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
    }
}
