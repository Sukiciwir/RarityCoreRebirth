package dev.raritycore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for parsing MiniMessage strings into Adventure {@link Component}s.
 */
public final class ColorUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Parses a MiniMessage string into a Component. */
    @NotNull
    public static Component parse(@NotNull String miniMessage) {
        return MM.deserialize(miniMessage);
    }

    /** Serializes a Component back to a MiniMessage string. */
    @NotNull
    public static String serialize(@NotNull Component component) {
        return MM.serialize(component);
    }

    /** Strips all MiniMessage tags from a string, returning plain text. */
    @NotNull
    public static String strip(@NotNull String miniMessage) {
        return MM.stripTags(miniMessage);
    }

    /**
     * Replaces placeholders in a MiniMessage template string.
     * Placeholder format: {@code {key}}.
     */
    @NotNull
    public static String replace(@NotNull String template, @NotNull String... pairs) {
        String result = template;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result = result.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return result;
    }

    private ColorUtil() {}
}
