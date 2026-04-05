package me.kyllian.paynowgui.fabric.gui;

import me.kyllian.paynowgui.core.utils.YMLFile;
import me.kyllian.paynowgui.fabric.FabricPayNowMod;
import me.kyllian.paynowgui.fabric.utils.FabricColorTranslator;
import me.kyllian.paynowgui.fabric.utils.FabricItemBuilder;
import me.kyllian.paynowgui.fabric.utils.MaterialMapper;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Base class for server-side Fabric GUIs using chest-style container inventories.
 * <p>
 * Opens a GenericContainerScreenHandler backed by a SimpleInventory.
 * Slot click actions are tracked via a map, similar to Bukkit's BasicInventory.
 */
public abstract class FabricBasicInventory {

    protected final FabricPayNowMod mod;
    protected final YMLFile config;
    protected final String sectionPath;

    protected SimpleInventory inventory;
    protected final Map<Integer, BiConsumer<ServerPlayerEntity, SlotActionType>> actions = new HashMap<>();

    private boolean closed = false;

    public FabricBasicInventory(FabricPayNowMod mod, String sectionPath) {
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
    public void open(ServerPlayerEntity player) {
        int slots = getSlotCount();
        // Clamp to valid chest row counts (9, 18, 27, 36, 45, 54)
        int rows = Math.max(1, Math.min(6, (slots + 8) / 9));
        int totalSlots = rows * 9;

        inventory = new SimpleInventory(totalSlots);
        buildInventory();

        String rawTitle = cfgString("title", "GUI");
        Text title = FabricColorTranslator.toText(titlePlaceholders(rawTitle));

        ScreenHandlerType<GenericContainerScreenHandler> handlerType = getHandlerType(rows);

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> {
                    GenericContainerScreenHandler handler = new GenericContainerScreenHandler(handlerType, syncId, playerInventory, inventory, rows) {
                        @Override
                        public void onSlotClick(int slotIndex, int button, SlotActionType actionType, net.minecraft.entity.player.PlayerEntity clickPlayer) {
                            // Prevent taking items
                            if (slotIndex >= 0 && slotIndex < totalSlots) {
                                BiConsumer<ServerPlayerEntity, SlotActionType> action = actions.get(slotIndex);
                                if (action != null && clickPlayer instanceof ServerPlayerEntity spe) {
                                    action.accept(spe, actionType);
                                }
                                return; // Cancel the click
                            }
                            // Also cancel clicks on player inventory
                        }

                        @Override
                        public void onClosed(net.minecraft.entity.player.PlayerEntity closingPlayer) {
                            super.onClosed(closingPlayer);
                            if (!closed) {
                                closed = true;
                                FabricBasicInventory.this.onClose(player);
                            }
                        }
                    };
                    return handler;
                },
                title
        ));
    }

    protected void addItem(int slot, ItemStack item) {
        if (inventory != null && slot >= 0 && slot < inventory.size()) {
            inventory.setStack(slot, item);
        }
    }

    protected void addItem(int slot, ItemStack item, BiConsumer<ServerPlayerEntity, SlotActionType> action) {
        addItem(slot, item);
        actions.put(slot, action);
    }

    protected void addItem(List<Integer> slots, ItemStack item) {
        for (int slot : slots) {
            addItem(slot, item);
        }
    }

    protected void addItem(List<Integer> slots, ItemStack item, BiConsumer<ServerPlayerEntity, SlotActionType> action) {
        for (int slot : slots) {
            addItem(slot, item);
            actions.put(slot, action);
        }
    }

    /**
     * Called when the inventory is closed.
     */
    protected void onClose(ServerPlayerEntity player) {
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

    private ScreenHandlerType<GenericContainerScreenHandler> getHandlerType(int rows) {
        return switch (rows) {
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> ScreenHandlerType.GENERIC_9X3;
        };
    }
}
