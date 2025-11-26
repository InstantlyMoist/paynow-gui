package me.kyllian.PayNowGUI.utils;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class YMLFile<P extends JavaPlugin> {

    @Getter
    private final P plugin;

    private final File file;

    @Getter
    private FileConfiguration fileConfiguration;

    public YMLFile(P plugin, String fileName) {
        this.plugin = plugin;

        if (!fileName.endsWith(".yml")) {
            throw new IllegalArgumentException("File name must end with .yml");
        }

        file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(fileName, false);
            } catch (Exception exception) {
                // Resource file doesn't exist, create a new one.
                try {
                    file.createNewFile();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        fileConfiguration = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            fileConfiguration.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
