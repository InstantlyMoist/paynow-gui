package me.kyllian.paynowgui.bukkit.platform;

import me.kyllian.paynowgui.core.platform.PlatformPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlayer implements PlatformPlayer {

    private final Player player;

    public BukkitPlayer(Player player) {
        this.player = player;
    }

    public Player getHandle() {
        return player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String getHostName() {
        return player.getAddress() != null ? player.getAddress().getHostName() : "unknown";
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public void closeGUI() {
        player.closeInventory();
    }
}
