package net.dungeonhd.jerryGunPlugin.item;

import net.dungeonhd.jerryGunPlugin.JerryGunPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static net.dungeonhd.jerryGunPlugin.util.Text.legacy;

/**
 * Builds and identifies the "Jerry-chine Gun" custom item, a recreation of
 * the Hypixel Skyblock Epic sword of the same name.
 */
public class JerrychineGunItem {

    private static final String ITEM_ID = "jerrychine_gun";

    // Rarity color for EPIC in Hypixel Skyblock is dark_purple (&5), not
    // light_purple (&d) - &d is reserved for MYTHIC. The item name and the
    // rarity tag at the bottom of the lore should match.
    private static final String DISPLAY_NAME = "&5Jerry-chine Gun";

    private static final String[] LORE_LINES = {
            "&7Damage: &c+80",
            "&7Intelligence: &b+200",
            " ",
            "&6Ability: Rapid-Fire  &e&lRIGHT CLICK",
            "&7Shoots a Jerry bullet, dealing",
            "&c1,538.4 &7damage on impact and",
            "&7knocking you back.",
            " ",
            "&7Each shot costs &b+30 mana &7more then",
            "&7the previous, resetting after &a4s &7of",
            "&7not firing",
            " ",
            "&8This item can be reforged!",
            "&5&lEPIC SWORD"
    };

    private final JerryGunPlugin plugin;
    private final NamespacedKey key;

    public JerrychineGunItem(JerryGunPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, ITEM_ID);
    }

    public NamespacedKey getKey() {
        return key;
    }

    /**
     * Creates a fresh ItemStack instance of the Jerry-chine Gun.
     */
    public ItemStack create() {
        Material material = resolveMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(legacy(DISPLAY_NAME));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>(LORE_LINES.length);
        for (String line : LORE_LINES) {
            lore.add(legacy(line));
        }
        meta.lore(lore);

        meta.setUnbreakable(true);
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_DYE
        );
        meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns true if the given stack is a Jerry-chine Gun created by this plugin.
     */
    public boolean isJerrychineGun(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }

    private Material resolveMaterial() {
        String name = plugin.getConfig().getString("item.material", "GOLDEN_HORSE_ARMOR");
        Material material = Material.matchMaterial(name);
        return material != null ? material : Material.GOLDEN_HORSE_ARMOR;
    }
}
