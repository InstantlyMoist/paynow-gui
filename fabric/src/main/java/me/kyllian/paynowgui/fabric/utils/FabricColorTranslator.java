package me.kyllian.paynowgui.fabric.utils;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates Bukkit-style '&' color codes to Minecraft Text components with Formatting.
 * Supports &0-&9, &a-&f, &k (obfuscated), &l (bold), &m (strikethrough), &n (underline), &o (italic), &r (reset).
 */
public class FabricColorTranslator {

    private static final Map<Character, Formatting> CODE_MAP = new HashMap<>();
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    static {
        CODE_MAP.put('0', Formatting.BLACK);
        CODE_MAP.put('1', Formatting.DARK_BLUE);
        CODE_MAP.put('2', Formatting.DARK_GREEN);
        CODE_MAP.put('3', Formatting.DARK_AQUA);
        CODE_MAP.put('4', Formatting.DARK_RED);
        CODE_MAP.put('5', Formatting.DARK_PURPLE);
        CODE_MAP.put('6', Formatting.GOLD);
        CODE_MAP.put('7', Formatting.GRAY);
        CODE_MAP.put('8', Formatting.DARK_GRAY);
        CODE_MAP.put('9', Formatting.BLUE);
        CODE_MAP.put('a', Formatting.GREEN);
        CODE_MAP.put('b', Formatting.AQUA);
        CODE_MAP.put('c', Formatting.RED);
        CODE_MAP.put('d', Formatting.LIGHT_PURPLE);
        CODE_MAP.put('e', Formatting.YELLOW);
        CODE_MAP.put('f', Formatting.WHITE);
        CODE_MAP.put('k', Formatting.OBFUSCATED);
        CODE_MAP.put('l', Formatting.BOLD);
        CODE_MAP.put('m', Formatting.STRIKETHROUGH);
        CODE_MAP.put('n', Formatting.UNDERLINE);
        CODE_MAP.put('o', Formatting.ITALIC);
        CODE_MAP.put('r', Formatting.RESET);
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
     * Convert a string with '&' color codes into a Minecraft Text component.
     */
    public static Text toText(String message) {
        if (message == null || message.isEmpty()) return Text.empty();

        MutableText result = Text.empty();
        Matcher matcher = COLOR_PATTERN.matcher(message);
        int lastEnd = 0;
        Style currentStyle = Style.EMPTY;

        while (matcher.find()) {
            // Append text before this code
            if (matcher.start() > lastEnd) {
                String segment = message.substring(lastEnd, matcher.start());
                result.append(Text.literal(segment).setStyle(currentStyle));
            }

            char code = Character.toLowerCase(matcher.group(1).charAt(0));
            Formatting formatting = CODE_MAP.get(code);
            if (formatting != null) {
                if (formatting == Formatting.RESET) {
                    currentStyle = Style.EMPTY;
                } else if (formatting.isColor()) {
                    // Color resets all formatting
                    currentStyle = Style.EMPTY.withFormatting(formatting);
                } else {
                    // Modifier stacks on top of current style
                    currentStyle = currentStyle.withFormatting(formatting);
                }
            }

            lastEnd = matcher.end();
        }

        // Append remaining text
        if (lastEnd < message.length()) {
            String remaining = message.substring(lastEnd);
            result.append(Text.literal(remaining).setStyle(currentStyle));
        }

        return result;
    }

    /**
     * Strip all '&' color codes from a string.
     */
    public static String stripCodes(String message) {
        if (message == null) return "";
        return COLOR_PATTERN.matcher(message).replaceAll("");
    }
}
