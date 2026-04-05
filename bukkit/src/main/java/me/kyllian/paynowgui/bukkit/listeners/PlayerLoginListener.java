package me.kyllian.paynowgui.bukkit.listeners;

import me.kyllian.paynowgui.bukkit.PayNowGUIPlugin;
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
        // Reserved for future use
    }
}
