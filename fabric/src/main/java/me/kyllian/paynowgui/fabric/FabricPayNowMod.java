package me.kyllian.paynowgui.fabric;

import lombok.Getter;
import me.kyllian.paynowgui.core.handlers.ProductHandler;
import me.kyllian.paynowgui.core.utils.StringUtils;
import me.kyllian.paynowgui.fabric.command.BuyCommand;
import me.kyllian.paynowgui.fabric.platform.FabricPlatform;
import me.kyllian.paynowgui.fabric.utils.FabricColorTranslator;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

@Getter
public class FabricPayNowMod implements DedicatedServerModInitializer {

    @Getter
    private static FabricPayNowMod instance;

    private MinecraftServer server;
    private FabricPlatform platform;
    private ProductHandler productHandler;

    @Override
    public void onInitializeServer() {
        instance = this;

        // Set up color translation for core's StringUtils
        StringUtils.setColorTranslator(FabricColorTranslator::translate);

        // Register the /buy command
        BuyCommand.register();

        // Initialize on server start (we need the MinecraftServer reference)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            this.platform = new FabricPlatform(this);
            this.productHandler = new ProductHandler(platform);

            platform.getLogger().info("[paynow-gui] Fabric mod initialized!");
        });
    }
}
