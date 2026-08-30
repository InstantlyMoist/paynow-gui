package me.kyllian.paynowgui.loader;

import lombok.Getter;
import me.kyllian.paynowgui.core.handlers.ProductHandler;
import me.kyllian.paynowgui.core.utils.StringUtils;
import me.kyllian.paynowgui.loader.command.BuyCommand;
import me.kyllian.paynowgui.loader.platform.LoaderPlatform;
import me.kyllian.paynowgui.loader.utils.ColorTranslator;
import net.minecraft.server.MinecraftServer;
//? if fabric {
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
*///?}

/**
 * Shared entrypoint for both loaders. The bootstrap differs (Fabric's
 * DedicatedServerModInitializer vs NeoForge's @Mod constructor), but everything
 * after {@link #bootstrap()} is identical.
 */
//? if neoforge
/*@Mod(value = "paynowgui", dist = Dist.DEDICATED_SERVER)*/
@Getter
public class PayNowLoaderMod
        //? if fabric
        implements DedicatedServerModInitializer
{

    @Getter
    private static PayNowLoaderMod instance;

    private MinecraftServer server;
    private LoaderPlatform platform;
    private ProductHandler productHandler;

    //? if fabric {
    @Override
    public void onInitializeServer() {
        bootstrap();
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
    }
    //?} else {
    /*public PayNowLoaderMod(IEventBus modBus, ModContainer container) {
        bootstrap();
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, e -> onServerStarted(e.getServer()));
    }
    *///?}

    private void bootstrap() {
        instance = this;
        // Teach core's StringUtils how to colourise on this platform.
        StringUtils.setColorTranslator(ColorTranslator::translate);
        BuyCommand.register();
    }

    /** Runs once the MinecraftServer instance exists. */
    private void onServerStarted(MinecraftServer server) {
        this.server = server;
        this.platform = new LoaderPlatform(this);
        this.productHandler = new ProductHandler(platform);
        platform.getLogger().info("[paynow-gui] initialized");
    }
}
