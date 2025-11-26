package me.kyllian.PayNowGUI.utils;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class StringUtils {

    public static String colorize(String message) {
        return translateAlternateColorCodes('&', message);
    }
}
