package me.kyllian.paynowgui.fabric.platform;

import lombok.Getter;
import me.kyllian.paynowgui.core.platform.PayNowPlatform;
import me.kyllian.paynowgui.core.platform.PlatformScheduler;
import me.kyllian.paynowgui.core.utils.YMLFile;
import me.kyllian.paynowgui.fabric.FabricPayNowMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class FabricPlatform implements PayNowPlatform {

    private final FabricPayNowMod mod;
    private final PlatformScheduler scheduler;
    private final File dataFolder;
    private final Logger logger;
    @Getter
    private YMLFile config;

    public FabricPlatform(FabricPayNowMod mod) {
        this.mod = mod;
        this.scheduler = new FabricScheduler(mod);
        this.dataFolder = FabricLoader.getInstance().getConfigDir().resolve("paynow-gui").toFile();
        this.logger = Logger.getLogger("paynow-gui");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        saveDefaultConfig();
        this.config = new YMLFile(dataFolder, "config.yml");
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public String getConfigString(String path) {
        return config.getString(path);
    }

    @Override
    public String getConfigString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    @Override
    public boolean getConfigBoolean(String path) {
        return config.getBoolean(path);
    }

    @Override
    public boolean getConfigBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    @Override
    public int getConfigInt(String path) {
        return config.getInt(path);
    }

    @Override
    public int getConfigInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Integer> getConfigIntList(String path) {
        Object value = config.get(path);
        if (value instanceof List<?> list) {
            List<Integer> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number number) {
                    result.add(number.intValue());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfigSection(String path) {
        Object value = config.get(path);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(k.toString(), v));
            return result;
        }
        return Collections.emptyMap();
    }

    @Override
    public boolean hasConfigPath(String path) {
        return config.has(path);
    }

    @Override
    public void reloadConfig() {
        config.reload();
    }

    @Override
    public void saveDefaultConfig() {
        // YMLFile constructor handles copying the resource if the file doesn't exist
        // We just need to ensure the data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

}
