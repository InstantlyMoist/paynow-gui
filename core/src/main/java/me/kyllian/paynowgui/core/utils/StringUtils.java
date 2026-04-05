package me.kyllian.paynowgui.core.utils;

/**
 * Platform-agnostic string utilities.
 * Color code translation is delegated to the platform.
 */
public class StringUtils {

    private static ColorTranslator colorTranslator = message -> message;

    public static void setColorTranslator(ColorTranslator translator) {
        colorTranslator = translator;
    }

    public static String colorize(String message) {
        return colorTranslator.translate(message);
    }

    @FunctionalInterface
    public interface ColorTranslator {
        String translate(String message);
    }
}
