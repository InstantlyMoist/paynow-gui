package me.kyllian.paynowgui.loader.platform;

import lombok.Getter;
import me.kyllian.paynowgui.core.platform.PlatformPlayer;
import me.kyllian.paynowgui.loader.utils.ColorTranslator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class LoaderPlayer implements PlatformPlayer {

    @Getter
    private final ServerPlayer handle;

    public LoaderPlayer(ServerPlayer player) {
        this.handle = player;
    }

    @Override
    public UUID getUniqueId() {
        return handle.getUUID();
    }

    @Override
    public String getName() {
        return handle.getName().getString();
    }

    @Override
    public String getHostName() {
        var address = handle.connection.getRemoteAddress();
        if (address instanceof java.net.InetSocketAddress inet) {
            return inet.getHostName();
        }
        return "unknown";
    }

    @Override
    public boolean isOnline() {
        return !handle.hasDisconnected();
    }

    @Override
    public boolean hasPermission(String permission) {
        // On vanilla Fabric, permission level 2 = OP
        // For basic permissions, we check OP status
        // For finer control, mods like fabric-permissions-api can be integrated later
        if (permission.equals("paynowgui.reload")) {
            // 1.21.11 replaced integer permission levels with PermissionSet.
            //? if >=1.21.11 {
            /*return handle.permissions().hasPermission(
                    new net.minecraft.server.permissions.Permission.HasCommandLevel(
                            net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS));
            *///?} else {
            return handle.hasPermissions(2);
            //?}
        }
        return true; // All players can use /buy by default
    }

    @Override
    public void sendMessage(String message) {
        handle.sendSystemMessage(ColorTranslator.toText(message));
    }

    @Override
    public void closeGUI() {
        handle.closeContainer();
    }
}
