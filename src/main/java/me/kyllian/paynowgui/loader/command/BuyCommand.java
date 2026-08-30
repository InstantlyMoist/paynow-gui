package me.kyllian.paynowgui.loader.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.kyllian.paynowgui.core.models.GUIPayload;
import me.kyllian.paynowgui.core.utils.Statistics;
import me.kyllian.paynowgui.loader.PayNowLoaderMod;
import me.kyllian.paynowgui.loader.gui.TagsMenu;
import me.kyllian.paynowgui.loader.utils.ColorTranslator;
//? if fabric {
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//?} else {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
*///?}
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public class BuyCommand {

    public static void register() {
        //? if fabric {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
        //?} else {
        /*NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> build(e.getDispatcher()));
        *///?}
    }

    /** The command tree itself is identical on both loaders. */
    private static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
        {
            dispatcher.register(
                    literal("buy")
                            .executes(BuyCommand::executeBuy)
                            .then(literal("reload")
                                    //? if >=1.21.11 {
                                    /*.requires(source -> source.permissions().hasPermission(
                                            new net.minecraft.server.permissions.Permission.HasCommandLevel(
                                                    net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS)))
                                    *///?} else {
                                    .requires(source -> source.hasPermission(2))
                                    //?}
                                    .executes(BuyCommand::executeReload))
            );
        }
    }

    private static int executeBuy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("You need to be a player to execute this command!"));
            return 0;
        }

        PayNowLoaderMod mod = PayNowLoaderMod.getInstance();
        if (mod == null || mod.getProductHandler() == null) {
            source.sendFailure(Component.literal("PayNow GUI is not yet initialized. Please wait for the server to fully start."));
            return 0;
        }

        Statistics.menuOpened.getAndIncrement();
        new TagsMenu(mod, player, new GUIPayload()).open(player);
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        PayNowLoaderMod mod = PayNowLoaderMod.getInstance();
        if (mod == null || mod.getProductHandler() == null) {
            context.getSource().sendFailure(Component.literal("PayNow GUI is not yet initialized."));
            return 0;
        }

        mod.getPlatform().reloadConfig();
        mod.getProductHandler().reload();
        mod.getProductHandler().loadProducts();
        context.getSource().sendSuccess(() -> Component.literal("paynow-gui configuration reloaded!").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GREEN)), false);
        return 1;
    }
}
