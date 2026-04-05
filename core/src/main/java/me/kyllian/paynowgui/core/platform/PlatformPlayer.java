package me.kyllian.paynowgui.core.platform;

import java.util.UUID;

/**
 * Platform-agnostic representation of a player.
 */
public interface PlatformPlayer {

    UUID getUniqueId();

    String getName();

    String getHostName();

    boolean isOnline();

    boolean hasPermission(String permission);

    void sendMessage(String message);

    void closeGUI();
}
