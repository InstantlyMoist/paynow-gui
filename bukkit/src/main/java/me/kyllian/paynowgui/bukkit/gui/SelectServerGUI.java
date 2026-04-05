package me.kyllian.paynowgui.bukkit.gui;

import gg.paynow.sdk.storefront.model.ProductTagDto;
import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.paynowgui.bukkit.PayNowGUIPlugin;
import me.kyllian.paynowgui.bukkit.platform.BukkitPlayer;
import me.kyllian.paynowgui.bukkit.utils.BasicInventory;
import me.kyllian.paynowgui.bukkit.utils.ItemBuilder;
import me.kyllian.paynowgui.core.models.GUIPayload;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SelectServerGUI extends BasicInventory<PayNowGUIPlugin> {

    private final Player player;
    private final BukkitPlayer platformPlayer;
    private final GUIPayload payload;
    private final ProductTagDto tag;
    private final StorefrontProductDto product;

    private boolean loading = false;

    public SelectServerGUI(PayNowGUIPlugin plugin, Player player, GUIPayload payload, ProductTagDto tag, StorefrontProductDto product) {
        super(plugin, plugin.getConfig(), "select_server_gui");

        this.player = player;
        this.platformPlayer = new BukkitPlayer(player);
        this.payload = payload;
        this.tag = tag;
        this.product = product;
    }

    @Override
    public void buildInventory() {
        product.getGameservers().forEach(server -> {
            ConfigurationSection itemSection = getSection().getConfigurationSection("items." + server.getId());
            if (itemSection == null) return;

            ItemStack serverItem = new ItemBuilder(Material.valueOf(itemSection.getString("material")))
                    .setName(itemSection.getString("name"))
                    .setLore(itemSection.getString("lore"))
                    .toItemStack();

            addItem(itemSection.getInt("slot"), serverItem, event -> {
                if (loading) return;
                loading = true;

                plugin.getProductHandler().setProductQuantityInCart(platformPlayer, server.getId(), product.getId(), 1, 1, (nothing) -> {
                    plugin.getProductHandler().getCart(platformPlayer, (fetchedCart) -> {
                        this.payload.setCart(fetchedCart);
                        player.openInventory(new ProductsGUI(plugin, player, payload, tag).getInventory());
                    });
                });
            });
        });
    }
}
