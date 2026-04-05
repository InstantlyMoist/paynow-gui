package me.kyllian.paynowgui.bukkit;

import lombok.Getter;
import me.kyllian.paynowgui.bukkit.executors.BuyExecutor;
import me.kyllian.paynowgui.bukkit.hooks.apollo.ApolloHook;
import me.kyllian.paynowgui.bukkit.hooks.apollo.IApolloHook;
import me.kyllian.paynowgui.bukkit.hooks.apollo.NoopApolloHook;
import me.kyllian.paynowgui.bukkit.hooks.npc.CitizensNpcHook;
import me.kyllian.paynowgui.bukkit.listeners.PlayerLoginListener;
import me.kyllian.paynowgui.bukkit.platform.BukkitPlatform;
import me.kyllian.paynowgui.core.handlers.ProductHandler;
import me.kyllian.paynowgui.core.handlers.RecentDonatorHandler;
import me.kyllian.paynowgui.core.hooks.INpcHook;
import me.kyllian.paynowgui.core.hooks.NoopNpcHook;
import me.kyllian.paynowgui.core.utils.Statistics;
import me.kyllian.paynowgui.core.utils.StringUtils;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import static me.kyllian.paynowgui.core.utils.Statistics.getAndReset;
import static org.bukkit.ChatColor.translateAlternateColorCodes;

@Getter
public class PayNowGUIPlugin extends JavaPlugin {

    private BukkitPlatform platform;

    private IApolloHook apolloHook;
    private INpcHook npcHook;

    private ProductHandler productHandler;
    private RecentDonatorHandler recentDonatorHandler;

    @Override
    public void onEnable() {
        // Initialize the platform-agnostic color translator
        StringUtils.setColorTranslator(message -> translateAlternateColorCodes('&', message));

        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        reloadConfig();

        platform = new BukkitPlatform(this);

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
        productHandler = new ProductHandler(platform);
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
        recentDonatorHandler = new RecentDonatorHandler(platform, npcHook);
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
