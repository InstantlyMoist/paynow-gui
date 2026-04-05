package me.kyllian.paynowgui.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.kyllian.paynowgui.core.models.GUIPayload;
import me.kyllian.paynowgui.core.utils.Statistics;
import me.kyllian.paynowgui.fabric.FabricPayNowMod;
import me.kyllian.paynowgui.fabric.gui.FabricTagsGUI;
import me.kyllian.paynowgui.fabric.utils.FabricColorTranslator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class BuyCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("buy")
                            .executes(BuyCommand::executeBuy)
                            .then(literal("reload")
                                    .requires(source -> source.hasPermissionLevel(2))
                                    .executes(BuyCommand::executeReload))
            );
        });
    }

    private static int executeBuy(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendError(Text.literal("You need to be a player to execute this command!"));
            return 0;
        }

        FabricPayNowMod mod = FabricPayNowMod.getInstance();
        if (mod == null || mod.getProductHandler() == null) {
            source.sendError(Text.literal("PayNow GUI is not yet initialized. Please wait for the server to fully start."));
            return 0;
        }

        Statistics.menuOpened.getAndIncrement();
        new FabricTagsGUI(mod, player, new GUIPayload()).open(player);
        return 1;
    }

    private static int executeReload(CommandContext<ServerCommandSource> context) {
        FabricPayNowMod mod = FabricPayNowMod.getInstance();
        if (mod == null || mod.getProductHandler() == null) {
            context.getSource().sendError(Text.literal("PayNow GUI is not yet initialized."));
            return 0;
        }

        mod.getPlatform().reloadConfig();
        mod.getProductHandler().reload();
        mod.getProductHandler().loadProducts();
        context.getSource().sendFeedback(() -> Text.literal("paynow-gui configuration reloaded!").styled(s -> s.withColor(net.minecraft.util.Formatting.GREEN)), false);
        return 1;
    }
}
