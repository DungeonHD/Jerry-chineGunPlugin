package net.dungeonhd.jerryGunPlugin.mana;

import net.dungeonhd.jerryGunPlugin.JerryGunPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hypixel Skyblock doesn't exist on a vanilla Paper server, so this plugin
 * ships its own minimal mana pool: every online player has mana that
 * regenerates over time and can be spent by the Jerry-chine Gun's ability.
 */
public class ManaManager {

    private final JerryGunPlugin plugin;
    private final Map<UUID, Double> mana = new ConcurrentHashMap<>();

    private final double maxMana;
    private final double regenPerSecond;

    private BukkitTask regenTask;
    private BukkitTask displayTask;

    public ManaManager(JerryGunPlugin plugin) {
        this.plugin = plugin;
        this.maxMana = plugin.getConfig().getDouble("mana.max", 1000.0);
        this.regenPerSecond = plugin.getConfig().getDouble("mana.regen-per-second", 40.0);
    }

    public void start() {
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::regenTick, 20L, 20L);
        displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::displayTick, 10L, 10L);
    }

    public void stop() {
        if (regenTask != null) {
            regenTask.cancel();
        }
        if (displayTask != null) {
            displayTask.cancel();
        }
    }

    private void regenTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            double current = mana.getOrDefault(player.getUniqueId(), maxMana);
            if (current < maxMana) {
                mana.put(player.getUniqueId(), Math.min(maxMana, current + regenPerSecond));
            }
        }
    }

    private void displayTick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isHoldingGun(player)) {
                double current = getMana(player);
                Component bar = Component.text("✎ Mana ", NamedTextColor.DARK_AQUA)
                        .append(Component.text((int) current + " / " + (int) maxMana, NamedTextColor.AQUA));
                player.sendActionBar(bar);
            }
        }
    }

    private boolean isHoldingGun(Player player) {
        return plugin.getGunItem().isJerrychineGun(player.getInventory().getItemInMainHand())
                || plugin.getGunItem().isJerrychineGun(player.getInventory().getItemInOffHand());
    }

    public double getMana(Player player) {
        return mana.getOrDefault(player.getUniqueId(), maxMana);
    }

    public double getMaxMana() {
        return maxMana;
    }

    public boolean has(Player player, double amount) {
        return getMana(player) >= amount;
    }

    public void remove(Player player, double amount) {
        mana.put(player.getUniqueId(), Math.max(0.0, getMana(player) - amount));
    }
}
