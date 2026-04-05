package me.kyllian.paynowgui.bukkit.platform;

import me.kyllian.paynowgui.core.platform.PayNowPlatform;
import me.kyllian.paynowgui.core.platform.PlatformScheduler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class BukkitPlatform implements PayNowPlatform {

    private final JavaPlugin plugin;
    private final PlatformScheduler scheduler;

    public BukkitPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new BukkitScheduler(plugin);
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public String getConfigString(String path) {
        return plugin.getConfig().getString(path);
    }

    @Override
    public String getConfigString(String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }

    @Override
    public boolean getConfigBoolean(String path) {
        return plugin.getConfig().getBoolean(path);
    }

    @Override
    public boolean getConfigBoolean(String path, boolean defaultValue) {
        return plugin.getConfig().getBoolean(path, defaultValue);
    }

    @Override
    public int getConfigInt(String path) {
        return plugin.getConfig().getInt(path);
    }

    @Override
    public int getConfigInt(String path, int defaultValue) {
        return plugin.getConfig().getInt(path, defaultValue);
    }

    @Override
    public List<Integer> getConfigIntList(String path) {
        return plugin.getConfig().getIntegerList(path);
    }

    @Override
    public Map<String, Object> getConfigSection(String path) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) return Collections.emptyMap();
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            result.put(key, section.get(key));
        }
        return result;
    }

    @Override
    public boolean hasConfigPath(String path) {
        return plugin.getConfig().contains(path);
    }

    @Override
    public void reloadConfig() {
        plugin.reloadConfig();
    }

    @Override
    public void saveDefaultConfig() {
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();
    }
}
