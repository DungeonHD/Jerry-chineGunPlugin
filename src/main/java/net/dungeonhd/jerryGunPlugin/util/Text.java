package net.dungeonhd.jerryGunPlugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Small helper around Adventure's legacy serializer so that '&'-coded
 * strings (as used by basically every Hypixel-Skyblock-style item) can be
 * turned into proper Components without the client auto-italicizing lore.
 */
public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    /**
     * Parses a '&'-coded string into a Component, forcing italics off so
     * lore/display names render exactly as written (Minecraft normally
     * forces lore lines italic unless told otherwise).
     */
    public static Component legacy(String raw) {
        return LEGACY.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
