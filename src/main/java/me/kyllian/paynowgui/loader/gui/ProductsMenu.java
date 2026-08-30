package me.kyllian.paynowgui.loader.gui;

import gg.paynow.sdk.storefront.model.CartLineDto;
import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.paynowgui.core.models.GUIPayload;
import me.kyllian.paynowgui.core.models.GUIProduct;
import me.kyllian.paynowgui.loader.PayNowLoaderMod;
import me.kyllian.paynowgui.loader.platform.LoaderPlayer;
import me.kyllian.paynowgui.loader.utils.ItemBuilder;
import me.kyllian.paynowgui.loader.utils.MaterialMapper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedList;
import java.util.List;

public class ProductsMenu extends BasicMenu {

    private final ServerPlayer player;
    private final LoaderPlayer platformPlayer;
    private final GUIPayload payload;

    private final ProductTagDto tag;
    private final List<StorefrontProductDto> products = new LinkedList<>();

    private boolean loading = false;

    public ProductsMenu(PayNowLoaderMod mod, ServerPlayer player, GUIPayload payload, ProductTagDto tag) {
        super(mod, "products_gui");
        this.player = player;
        this.platformPlayer = new LoaderPlayer(player);
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
        int backSlot = inventory.getContainerSize() - 5;
        if (cfgHas("back_item")) {
            ItemStack backItem = new ItemBuilder(cfgString("back_item.material"))
                    .setName(cfgString("back_item.name"))
                    .toItemStack();
            addItem(backSlot, backItem, (clickPlayer, button, actionType) -> {
                clickPlayer.closeContainer();
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
            int count = Math.max(1, Math.min(inCart, material.getDefaultMaxStackSize()));

            ItemStack productItem = new ItemBuilder(material, count)
                    .setName(guiProduct.getDisplayName())
                    .setCustomModelData(guiProduct.getCustomModelData())
                    .setLore(cfgString("item.lore", "")
                            .replace("%price%", String.format("%.2f", product.getPrice() / 100.0))
                            .replace("%amount%", String.valueOf(inCart)))
                    .setEnchanted(inCart != 0)
                    .toItemStack();

            final int currentInCart = inCart;
            final boolean isFull = fullFilled;

            addItem(i, productItem, (clickPlayer, button, actionType) -> {
                if (loading) return;

                Object gameServerId = null;
                if (product.getSingleGameServerOnly()) {
                    if (currentInCart == 0) {
                        loading = true;
                        new SelectServerMenu(mod, clickPlayer, payload, tag, product).open(clickPlayer);
                        return;
                    }
                    CartLineDto line = payload.getCart().getLines().stream()
                            .filter(l -> l.getProductId().equals(product.getId()))
                            .findFirst()
                            .orElse(null);
                    gameServerId = line != null ? line.getSelectedGameserverId() : null;
                }

                // Mirrors the Bukkit GUI: left = +1, right = -1, shift+right = clear.
                boolean shift = actionType == ClickType.QUICK_MOVE;
                boolean rightClick = button == 1;

                if (shift && rightClick && currentInCart != 0)
                    setQuantity(product, 0, gameServerId, -currentInCart);
                else if (!shift && !rightClick && !isFull)
                    setQuantity(product, currentInCart + 1, gameServerId, 1);
                else if (!shift && rightClick && currentInCart != 0)
                    setQuantity(product, currentInCart - 1, gameServerId, -1);
            });
        }
    }

    private void setQuantity(StorefrontProductDto product, int quantity, Object gameServerId, int delta) {
        loading = true;
        mod.getProductHandler().setProductQuantityInCart(platformPlayer, gameServerId, product.getId(), quantity, delta, (nothing) -> {
            mod.getProductHandler().getCart(platformPlayer, (fetchedCart) -> {
                this.payload.setCart(fetchedCart);
                new ProductsMenu(mod, player, payload, tag).open(player);
            });
        });
    }

    @Override
    protected void onClose(ServerPlayer closingPlayer) {
        if (loading) return;
        // Re-open tags GUI when closing products GUI (same as Bukkit behavior)
        mod.getPlatform().getScheduler().runSyncLater(() ->
                new TagsMenu(mod, closingPlayer, payload).open(closingPlayer), 1L);
    }
}
