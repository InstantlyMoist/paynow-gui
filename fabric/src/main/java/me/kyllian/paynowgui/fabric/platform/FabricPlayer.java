package me.kyllian.paynowgui.fabric.platform;

import lombok.Getter;
import me.kyllian.paynowgui.core.platform.PlatformPlayer;
import me.kyllian.paynowgui.fabric.utils.FabricColorTranslator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class FabricPlayer implements PlatformPlayer {

    @Getter
    private final ServerPlayerEntity handle;

    public FabricPlayer(ServerPlayerEntity player) {
        this.handle = player;
    }

    @Override
    public UUID getUniqueId() {
        return handle.getUuid();
    }

    @Override
    public String getName() {
        return handle.getName().getString();
    }

    @Override
    public String getHostName() {
        var address = handle.networkHandler.getConnectionAddress();
        if (address instanceof java.net.InetSocketAddress inet) {
            return inet.getHostName();
        }
        return "unknown";
    }

    @Override
    public boolean isOnline() {
        return !handle.isDisconnected();
    }

    @Override
    public boolean hasPermission(String permission) {
        // On vanilla Fabric, permission level 2 = OP
        // For basic permissions, we check OP status
        // For finer control, mods like fabric-permissions-api can be integrated later
        if (permission.equals("paynowgui.reload")) {
            return handle.hasPermissionLevel(2);
        }
        return true; // All players can use /buy by default
    }

    @Override
    public void sendMessage(String message) {
        handle.sendMessage(FabricColorTranslator.toText(message));
    }

    @Override
    public void closeGUI() {
        handle.closeHandledScreen();
    }
}
