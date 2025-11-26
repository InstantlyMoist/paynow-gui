package me.kyllian.PayNowGUI.executors;

import me.kyllian.PayNowGUI.PayNowGUIPlugin;
import me.kyllian.PayNowGUI.gui.TagsGUI;
import me.kyllian.PayNowGUI.models.GUIPayload;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyExecutor implements CommandExecutor {

    private final PayNowGUIPlugin plugin;

    public BuyExecutor(PayNowGUIPlugin plugin) {
        this.plugin = plugin;

        plugin.getCommand("buy").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(ChatColor.RED + "You need to be a player to execute this command!");
            return true;
        }

        player.openInventory(new TagsGUI(plugin, player, new GUIPayload()).getInventory());
        return true;
    }
}
