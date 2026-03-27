package me.kyllian.PayNowGUI;

import lombok.Getter;
import me.kyllian.PayNowGUI.executors.BuyExecutor;
import me.kyllian.PayNowGUI.handlers.ProductHandler;
import me.kyllian.PayNowGUI.handlers.RecentDonatorHandler;
import me.kyllian.PayNowGUI.hooks.apollo.ApolloHook;
import me.kyllian.PayNowGUI.hooks.apollo.IApolloHook;
import me.kyllian.PayNowGUI.hooks.apollo.NoopApolloHook;
import me.kyllian.PayNowGUI.hooks.npc.CitizensNpcHook;
import me.kyllian.PayNowGUI.hooks.npc.INpcHook;
import me.kyllian.PayNowGUI.hooks.npc.NoopNpcHook;
import me.kyllian.PayNowGUI.listeners.PlayerLoginListener;
import me.kyllian.PayNowGUI.utils.Statistics;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import static me.kyllian.PayNowGUI.utils.Statistics.getAndReset;

@Getter
public class PayNowGUIPlugin extends JavaPlugin {

    private IApolloHook apolloHook;
    private INpcHook npcHook;

    private ProductHandler productHandler;
    private RecentDonatorHandler recentDonatorHandler;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        reloadConfig();

        new PlayerLoginListener(this);

        initExecutors();
        initHandlers();
        initApolloHook();
        initNpcHook();
        initMetrics();
    }

    private void initExecutors() {
        new BuyExecutor(this);
    }

    private void initHandlers() {
        productHandler = new ProductHandler(this);
    }

    public void initApolloHook() {
        if (getServer().getPluginManager().getPlugin("Apollo-Bukkit") != null && getConfig().getBoolean("apollo_hook")) {
            Bukkit.getLogger().info("[paynow-gui] Enabling Apollo-Bukkit hook!");
            apolloHook = new ApolloHook();
        } else {
            Bukkit.getLogger().info("[paynow-gui] Apollo-Bukkit not detected!");
            apolloHook = new NoopApolloHook();
        }
    }

    public void initNpcHook() {
        if (getServer().getPluginManager().getPlugin("Citizens") != null && getConfig().getBoolean("recent_donator_npc.enabled")) {
            Bukkit.getLogger().info("[paynow-gui] Enabling Citizens NPC hook!");
            npcHook = new CitizensNpcHook();
        } else {
            Bukkit.getLogger().info("[paynow-gui] Citizens not detected or recent_donator_npc disabled!");
            npcHook = new NoopNpcHook();
        }
        recentDonatorHandler = new RecentDonatorHandler(this, npcHook);
    }

    private void initMetrics() {
        Metrics metrics = new Metrics(this, 28393);

        metrics.addCustomChart(new SingleLineChart("totalProducts", () -> Statistics.products));
        metrics.addCustomChart(new SingleLineChart("totalTags", () -> Statistics.tags));

        metrics.addCustomChart(new SingleLineChart("menuOpened", () -> getAndReset(Statistics.menuOpened)));
        metrics.addCustomChart(new SingleLineChart("cartsOpened", () -> getAndReset(Statistics.cartsOpened)));
        metrics.addCustomChart(new SingleLineChart("lunarCartsOpened", () -> getAndReset(Statistics.lunarCartsOpened)));

        metrics.addCustomChart(new SingleLineChart("cartsCleared", () -> getAndReset(Statistics.cartsCleared)));
        metrics.addCustomChart(new SingleLineChart("productsAdded", () -> getAndReset(Statistics.productsAdded)));
        metrics.addCustomChart(new SingleLineChart("productsRemoved", () -> getAndReset(Statistics.productsRemoved)));
    }
}
