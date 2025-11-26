package me.kyllian.PayNowGUI.gui;

import gg.paynow.sdk.storefront.model.StorefrontProductDto;
import me.kyllian.PayNowGUI.PayNowGUIPlugin;
import me.kyllian.PayNowGUI.models.GUIPayload;
import me.kyllian.PayNowGUI.utils.BasicInventory;
import me.kyllian.PayNowGUI.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SelectServerGUI extends BasicInventory<PayNowGUIPlugin> {

    private final Player player;
    private final GUIPayload payload;
    private final StorefrontProductDto product;

    private boolean loading = false;

    public SelectServerGUI(PayNowGUIPlugin plugin, Player player, GUIPayload payload, StorefrontProductDto product) {
        super(plugin, plugin.getConfig(), "select_server_gui");

        this.player = player;
        this.payload = payload;
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

            addItem(itemSection.getInt("slot"), serverItem, e -> {
                Bukkit.broadcastMessage("clicked " + server.getName());
            });
        });
    }
}
