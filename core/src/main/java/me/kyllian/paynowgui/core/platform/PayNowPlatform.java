package me.kyllian.paynowgui.core.platform;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Platform-agnostic plugin/mod entry point abstraction.
 */
public interface PayNowPlatform {

    PlatformScheduler getScheduler();

    Logger getLogger();

    File getDataFolder();

    /**
     * Get a configuration value by path (dot-separated).
     */
    String getConfigString(String path);

    String getConfigString(String path, String defaultValue);

    boolean getConfigBoolean(String path);

    boolean getConfigBoolean(String path, boolean defaultValue);

    int getConfigInt(String path);

    int getConfigInt(String path, int defaultValue);

    List<Integer> getConfigIntList(String path);

    /**
     * Get a configuration section as a map of key-value pairs.
     */
    Map<String, Object> getConfigSection(String path);

    /**
     * Check if a configuration path exists.
     */
    boolean hasConfigPath(String path);

    /**
     * Reload the configuration from disk.
     */
    void reloadConfig();

    /**
     * Save the default configuration if it doesn't exist.
     */
    void saveDefaultConfig();
}
