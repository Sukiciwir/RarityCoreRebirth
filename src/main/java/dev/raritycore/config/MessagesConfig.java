package dev.raritycore.config;

import dev.raritycore.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Typed wrapper around {@code messages.yml} providing player-facing messages.
 * All strings are in MiniMessage format with {@code {prefix}} resolved.
 */
public final class MessagesConfig {

    private final String prefix;
    private final FileConfiguration cfg;

    public MessagesConfig(@NotNull String prefix, @NotNull FileConfiguration cfg) {
        this.prefix = prefix;
        this.cfg = cfg;
    }

    /** Returns a MiniMessage-parsed Component for the given key, resolving {prefix}. */
    @NotNull
    public Component get(@NotNull String key, @NotNull String... replacements) {
        String raw = cfg.getString("messages." + key, "<red>Missing message: " + key);
        raw = raw.replace("{prefix}", prefix);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return ColorUtil.parse(raw);
    }

    /** Returns the raw MiniMessage string (prefix resolved, placeholders applied). */
    @NotNull
    public String getRaw(@NotNull String key, @NotNull String... replacements) {
        String raw = cfg.getString("messages." + key, "<red>Missing message: " + key);
        raw = raw.replace("{prefix}", prefix);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return raw;
    }
}
