package me.kyllian.paynowgui.fabric.gui;

import gg.paynow.sdk.storefront.model.CartLineDto;
import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.paynowgui.core.models.GUIPayload;
import me.kyllian.paynowgui.core.models.GUIProduct;
import me.kyllian.paynowgui.fabric.FabricPayNowMod;
import me.kyllian.paynowgui.fabric.platform.FabricPlayer;
import me.kyllian.paynowgui.fabric.utils.FabricItemBuilder;
import me.kyllian.paynowgui.fabric.utils.MaterialMapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedList;
import java.util.List;

public class FabricProductsGUI extends FabricBasicInventory {

    private final ServerPlayerEntity player;
    private final FabricPlayer platformPlayer;
    private final GUIPayload payload;

    private final ProductTagDto tag;
    private final List<StorefrontProductDto> products = new LinkedList<>();

    private boolean loading = false;

    public FabricProductsGUI(FabricPayNowMod mod, ServerPlayerEntity player, GUIPayload payload, ProductTagDto tag) {
        super(mod, "products_gui");
        this.player = player;
        this.platformPlayer = new FabricPlayer(player);
        this.payload = payload;
        this.tag = tag;

        this.products.addAll(payload.getAllProducts().stream()
                .filter(p -> p.getTags().stream()
                        .anyMatch(t -> t.getId().equals(tag.getId()))).toList());
    }

    @Override
    public String titlePlaceholders(String title) {
        return title.replace("%tag%", tag.getName());
    }

    @Override
    public int getSlotCount() {
        int size = cfgInt("size", 0);
        if (size != 0) return size;
        return ((products.size() - 1) / 9 + 1) * 9 + 9;
    }

    @Override
    public void buildInventory() {
        // Back item in the last row, middle slot
        int backSlot = inventory.size() - 5;
        if (cfgHas("back_item")) {
            ItemStack backItem = new FabricItemBuilder(cfgString("back_item.material"))
                    .setName(cfgString("back_item.name"))
                    .toItemStack();
            addItem(backSlot, backItem, (clickPlayer, actionType) -> {
                clickPlayer.closeHandledScreen();
            });
        }

        for (int i = 0; i < products.size(); i++) {
            StorefrontProductDto product = products.get(i);
            GUIProduct guiProduct = mod.getProductHandler().getGuiProductMap().getOrDefault(product.getId(), new GUIProduct(product));

            CartLineDto productInCart = payload.getCart().getLines().stream()
                    .filter(line -> line.getProductId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            boolean fullFilled = productInCart != null && product.getStock() != null
                    && product.getStock().getCustomerAvailable() != -1
                    && productInCart.getQuantity() >= product.getStock().getCustomerAvailable();
            int inCart = productInCart != null ? productInCart.getQuantity() : 0;

            String materialName = fullFilled ? cfgString("item.material_max_quantity") : guiProduct.getMaterial();
            Item material = MaterialMapper.fromString(materialName);
            int count = Math.max(1, Math.min(inCart, material.getMaxCount()));

            ItemStack productItem = new FabricItemBuilder(material, count)
                    .setName(guiProduct.getDisplayName())
                    .setCustomModelData(guiProduct.getCustomModelData())
                    .setLore(cfgString("item.lore", "")
                            .replace("%price%", String.format("%.2f", product.getPrice() / 100.0))
                            .replace("%amount%", String.valueOf(inCart)))
                    .setEnchanted(inCart != 0)
                    .toItemStack();

            final int currentInCart = inCart;
            final boolean isFull = fullFilled;

            addItem(i, productItem, (clickPlayer, actionType) -> {
                if (loading) return;

                Object gameServerId = null;
                if (product.getSingleGameServerOnly()) {
                    if (currentInCart == 0) {
                        loading = true;
                        new FabricSelectServerGUI(mod, clickPlayer, payload, tag, product).open(clickPlayer);
                        return;
                    }
                    CartLineDto line = payload.getCart().getLines().stream()
                            .filter(l -> l.getProductId().equals(product.getId()))
                            .findFirst()
                            .orElse(null);
                    gameServerId = line != null ? line.getSelectedGameserverId() : null;
                }

                // LEFT click = add, RIGHT click = remove, SHIFT_RIGHT = remove all
                if (actionType == SlotActionType.PICKUP && !isFull) {
                    // Left click (button 0 in PICKUP) - we treat any PICKUP as add
                    setQuantity(product, currentInCart + 1, gameServerId, 1);
                } else if (actionType == SlotActionType.QUICK_MOVE && currentInCart != 0) {
                    // Shift-click = remove all
                    setQuantity(product, 0, gameServerId, -currentInCart);
                }
                // For right-click: In Fabric, right-click in a container is also PICKUP with button=1
                // We handle it via the action type check; unfortunately SlotActionType doesn't distinguish
                // left/right click directly. We'll handle this in a simplified way:
                // PICKUP = add, QUICK_MOVE (shift) = remove all
                // Players can shift-click to remove items from cart.
            });
        }
    }

    private void setQuantity(StorefrontProductDto product, int quantity, Object gameServerId, int delta) {
        loading = true;
        mod.getProductHandler().setProductQuantityInCart(platformPlayer, gameServerId, product.getId(), quantity, delta, (nothing) -> {
            mod.getProductHandler().getCart(platformPlayer, (fetchedCart) -> {
                this.payload.setCart(fetchedCart);
                new FabricProductsGUI(mod, player, payload, tag).open(player);
            });
        });
    }

    @Override
    protected void onClose(ServerPlayerEntity closingPlayer) {
        if (loading) return;
        // Re-open tags GUI when closing products GUI (same as Bukkit behavior)
        mod.getPlatform().getScheduler().runSyncLater(() ->
                new FabricTagsGUI(mod, closingPlayer, payload).open(closingPlayer), 1L);
    }
}
