package me.kyllian.PayNowGUI.listeners;

import me.kyllian.PayNowGUI.PayNowGUIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerLoginListener implements Listener {

    private final PayNowGUIPlugin plugin;

    public PlayerLoginListener(PayNowGUIPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
//        Player player = event.getPlayer();
//        Basket basket = plugin.getBasketHandler().getBasket(player);
//        if (basket.getId() == -1) return;
//        player.sendMessage(StringUtils.colorize(plugin.getConfig().getString("messages.cart_unpurchased_login").replace("%link%", basket.getCheckoutUrl())));

    }

}
