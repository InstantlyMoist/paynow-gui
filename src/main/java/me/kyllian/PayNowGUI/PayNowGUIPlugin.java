package me.kyllian.PayNowGUI;

import lombok.Getter;
import me.kyllian.PayNowGUI.executors.BuyExecutor;
import me.kyllian.PayNowGUI.handlers.ProductHandler;
import me.kyllian.PayNowGUI.hooks.apollo.ApolloHook;
import me.kyllian.PayNowGUI.hooks.apollo.IApolloHook;
import me.kyllian.PayNowGUI.hooks.apollo.NoopApolloHook;
import me.kyllian.PayNowGUI.listeners.PlayerLoginListener;
import me.kyllian.PayNowGUI.utils.Statistics;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
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
        initMetrics();
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

    private void initMetrics() {
        Metrics metrics = new Metrics(this, 28393);

        metrics.addCustomChart(new SingleLineChart("totalProducts", () -> Statistics.products));
        metrics.addCustomChart(new SingleLineChart("totalTags", () -> Statistics.tags));

        metrics.addCustomChart(new SingleLineChart("menuOpened", () -> Statistics.menuOpened));
        metrics.addCustomChart(new SingleLineChart("cartsOpened", () -> Statistics.cartsOpened));
        metrics.addCustomChart(new SingleLineChart("lunarCartsOpened", () -> Statistics.lunarCartsOpened));

        metrics.addCustomChart(new SingleLineChart("cartsCleared", () -> Statistics.cartsCleared));
        metrics.addCustomChart(new SingleLineChart("productsAdded", () -> Statistics.productsAdded));
        metrics.addCustomChart(new SingleLineChart("productsRemoved", () -> Statistics.productsRemoved));
    }
}
