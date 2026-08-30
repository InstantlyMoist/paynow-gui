package me.kyllian.paynowgui.loader.utils;

import net.minecraft.network.chat.*;
import net.minecraft.ChatFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates Bukkit-style '&' color codes to Minecraft Component components with ChatFormatting.
 * Supports &0-&9, &a-&f, &k (obfuscated), &l (bold), &m (strikethrough), &n (underline), &o (italic), &r (reset).
 */
public class ColorTranslator {

    private static final Map<Character, ChatFormatting> CODE_MAP = new HashMap<>();
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    static {
        CODE_MAP.put('0', ChatFormatting.BLACK);
        CODE_MAP.put('1', ChatFormatting.DARK_BLUE);
        CODE_MAP.put('2', ChatFormatting.DARK_GREEN);
        CODE_MAP.put('3', ChatFormatting.DARK_AQUA);
        CODE_MAP.put('4', ChatFormatting.DARK_RED);
        CODE_MAP.put('5', ChatFormatting.DARK_PURPLE);
        CODE_MAP.put('6', ChatFormatting.GOLD);
        CODE_MAP.put('7', ChatFormatting.GRAY);
        CODE_MAP.put('8', ChatFormatting.DARK_GRAY);
        CODE_MAP.put('9', ChatFormatting.BLUE);
        CODE_MAP.put('a', ChatFormatting.GREEN);
        CODE_MAP.put('b', ChatFormatting.AQUA);
        CODE_MAP.put('c', ChatFormatting.RED);
        CODE_MAP.put('d', ChatFormatting.LIGHT_PURPLE);
        CODE_MAP.put('e', ChatFormatting.YELLOW);
        CODE_MAP.put('f', ChatFormatting.WHITE);
        CODE_MAP.put('k', ChatFormatting.OBFUSCATED);
        CODE_MAP.put('l', ChatFormatting.BOLD);
        CODE_MAP.put('m', ChatFormatting.STRIKETHROUGH);
        CODE_MAP.put('n', ChatFormatting.UNDERLINE);
        CODE_MAP.put('o', ChatFormatting.ITALIC);
        CODE_MAP.put('r', ChatFormatting.RESET);
    }

    /**
     * Translate '&' color codes to section symbol (§) for use with legacy text.
     * This is the simplest approach and works for StringUtils.ColorTranslator.
     */
    public static String translate(String message) {
        if (message == null) return "";
        return message.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    /**
     * Convert a string with '&' color codes into a Minecraft Component component.
     */
    public static Component toText(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        MutableComponent result = Component.empty();
        Matcher matcher = COLOR_PATTERN.matcher(message);
        int lastEnd = 0;
        Style currentStyle = Style.EMPTY;

        while (matcher.find()) {
            // Append text before this code
            if (matcher.start() > lastEnd) {
                String segment = message.substring(lastEnd, matcher.start());
                result.append(Component.literal(segment).setStyle(currentStyle));
            }

            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            // Classify by the code itself: ChatFormatting lost isColor() in 26.1, and the
            // per-modifier setters below are stable across the whole 1.20.5-26.x range.
            ChatFormatting formatting = CODE_MAP.get(code);
            if (formatting != null) {
                if (code == 'r') {
                    currentStyle = Style.EMPTY;
                } else if (isColorCode(code)) {
                    // A colour resets all active formatting, matching vanilla legacy behaviour.
                    currentStyle = Style.EMPTY.withColor(formatting);
                } else {
                    currentStyle = switch (code) {
                        case 'l' -> currentStyle.withBold(true);
                        case 'o' -> currentStyle.withItalic(true);
                        case 'n' -> currentStyle.withUnderlined(true);
                        case 'm' -> currentStyle.withStrikethrough(true);
                        case 'k' -> currentStyle.withObfuscated(true);
                        default -> currentStyle;
                    };
                }
            }

            lastEnd = matcher.end();
        }

        // Append remaining text
        if (lastEnd < message.length()) {
            String remaining = message.substring(lastEnd);
            result.append(Component.literal(remaining).setStyle(currentStyle));
        }

        return result;
    }

    private static boolean isColorCode(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    /**
     * Strip all '&' color codes from a string.
     */
    public static String stripCodes(String message) {
        if (message == null) return "";
        return COLOR_PATTERN.matcher(message).replaceAll("");
    }
}
