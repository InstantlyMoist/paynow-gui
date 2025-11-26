package me.kyllian.PayNowGUI;

import lombok.Getter;
import me.kyllian.PayNowGUI.executors.BuyExecutor;
import me.kyllian.PayNowGUI.handlers.ProductHandler;
import me.kyllian.PayNowGUI.hooks.apollo.ApolloHook;
import me.kyllian.PayNowGUI.hooks.apollo.IApolloHook;
import me.kyllian.PayNowGUI.hooks.apollo.NoopApolloHook;
import me.kyllian.PayNowGUI.listeners.PlayerLoginListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class PayNowGUIPlugin extends JavaPlugin {

    private IApolloHook apolloHook;

    private ProductHandler productHandler;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        reloadConfig();

        new PlayerLoginListener(this);

        initExecutors();
        initHandlers();
        initApolloHook();
    }

    private void initExecutors() {
        new BuyExecutor(this);
    }

    private void initHandlers() {
        productHandler = new ProductHandler(this);
    }

    private void initApolloHook() {
        if (getServer().getPluginManager().getPlugin("Apollo-Bukkit") != null && getConfig().getBoolean("apollo_hook")) {
            Bukkit.getLogger().info("[paynow-gui] Enabling Apollo-Bukkit hook!");
            apolloHook = new ApolloHook();
        } else {
            Bukkit.getLogger().info("[paynow-gui] Apollo-Bukkit not detected!");
            apolloHook = new NoopApolloHook();
        }
    }
}
