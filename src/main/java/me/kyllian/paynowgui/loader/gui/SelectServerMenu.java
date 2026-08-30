package me.kyllian.paynowgui.loader.gui;

import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.paynowgui.core.models.GUIPayload;
import me.kyllian.paynowgui.loader.PayNowLoaderMod;
import me.kyllian.paynowgui.loader.platform.LoaderPlayer;
import me.kyllian.paynowgui.loader.utils.ItemBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public class SelectServerMenu extends BasicMenu {

    private final ServerPlayer player;
    private final LoaderPlayer platformPlayer;
    private final GUIPayload payload;
    private final ProductTagDto tag;
    private final StorefrontProductDto product;

    private boolean loading = false;

    public SelectServerMenu(PayNowLoaderMod mod, ServerPlayer player, GUIPayload payload, ProductTagDto tag, StorefrontProductDto product) {
        super(mod, "select_server_gui");
        this.player = player;
        this.platformPlayer = new LoaderPlayer(player);
        this.payload = payload;
        this.tag = tag;
        this.product = product;
    }

    @Override
    public int getSlotCount() {
        // Config may have "type: HOPPER" for Bukkit (5 slots).
        // On Fabric, we use the smallest chest (9 slots) as a substitute for hopper.
        String type = cfgString("type", "");
        if (type.equalsIgnoreCase("HOPPER")) {
            return 9; // Smallest chest row
        }
        return cfgInt("size", 9);
    }

    @Override
    public void buildInventory() {
        product.getGameservers().forEach(server -> {
            String serverId = server.getId().toString();
            String itemPath = "items." + serverId + ".";

            if (!cfgHas("items." + serverId)) return;

            int slot = cfgInt(itemPath + "slot");
            ItemStack serverItem = new ItemBuilder(cfgString(itemPath + "material"))
                    .setName(cfgString(itemPath + "name"))
                    .setLore(cfgString(itemPath + "lore"))
                    .toItemStack();

            addItem(slot, serverItem, (clickPlayer, button, actionType) -> {
                if (loading) return;
                loading = true;

                mod.getProductHandler().setProductQuantityInCart(platformPlayer, server.getId(), product.getId(), 1, 1, (nothing) -> {
                    mod.getProductHandler().getCart(platformPlayer, (fetchedCart) -> {
                        this.payload.setCart(fetchedCart);
                        new ProductsMenu(mod, clickPlayer, payload, tag).open(clickPlayer);
                    });
                });
            });
        });
    }
}
