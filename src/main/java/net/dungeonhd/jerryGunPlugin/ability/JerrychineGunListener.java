package net.dungeonhd.jerryGunPlugin.ability;

import net.dungeonhd.jerryGunPlugin.JerryGunPlugin;
import net.dungeonhd.jerryGunPlugin.mana.ManaManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.dungeonhd.jerryGunPlugin.util.Text.legacy;

/**
 * Handles the "Rapid-Fire" right-click ability, including the Hypixel-style
 * scaling mana cost (+30 mana per consecutive shot, resetting after 4s of
 * not firing) and the hard 5 shots/second fire rate cap.
 */
public class JerrychineGunListener implements Listener {

    private final JerryGunPlugin plugin;

    private final Map<UUID, Long> lastShotMillis = new HashMap<>();
    private final Map<UUID, Double> lastCost = new HashMap<>();

    private final double baseCost;
    private final double costIncrement;
    private final long resetAfterMillis;
    private final long fireRateCooldownMillis;

    private final double abilityDamage;
    private final double bulletSpeedBlocksPerSecond;
    private final double maxRange;
    private final double knockbackRadius;
    private final double knockbackUpward;
    private final double knockbackHorizontal;
    private final double spreadDegrees;
    private final boolean allowWallPiercing;
    private final String skullTexture;

    public JerrychineGunListener(JerryGunPlugin plugin) {
        this.plugin = plugin;

        this.baseCost = plugin.getConfig().getDouble("mana.base-cost", 30.0);
        this.costIncrement = plugin.getConfig().getDouble("mana.cost-increment", 30.0);
        this.resetAfterMillis = (long) (plugin.getConfig().getDouble("mana.reset-after-seconds", 4.0) * 1000L);
        this.fireRateCooldownMillis = plugin.getConfig().getLong("ability.fire-rate-cooldown-ms", 200L);

        this.abilityDamage = plugin.getConfig().getDouble("ability.damage", 1538.4);
        this.bulletSpeedBlocksPerSecond = plugin.getConfig().getDouble("ability.bullet-speed-blocks-per-second", 45.0);
        this.maxRange = plugin.getConfig().getDouble("ability.max-range", 40.0);
        this.knockbackRadius = plugin.getConfig().getDouble("ability.knockback-radius", 2.5);
        this.knockbackUpward = plugin.getConfig().getDouble("ability.knockback-upward", 2.5);
        this.knockbackHorizontal = plugin.getConfig().getDouble("ability.knockback-horizontal", 2.5);
        this.spreadDegrees = plugin.getConfig().getDouble("ability.spread-degrees", 3.0);
        this.allowWallPiercing = plugin.getConfig().getBoolean("ability.allow-wall-piercing", true);
        this.skullTexture = plugin.getConfig().getString("ability.bullet-skull-texture", "");
    }

    // IMPORTANT: no ignoreCancelled here! Items without a vanilla "use"
    // action (swords, horse armor, ...) make the server fire this event
    // PRE-CANCELLED for RIGHT_CLICK_AIR (see PlayerInteractEvent javadoc:
    // "will fire as cancelled if the vanilla behavior is to do nothing").
    // With ignoreCancelled = true we'd only ever see RIGHT_CLICK_BLOCK,
    // which is why the ability previously only worked while looking at a
    // nearby block.
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getGunItem().isJerrychineGun(item)) {
            return;
        }

        event.setCancelled(true);
        tryFire(player, item);
    }

    private void tryFire(Player player, ItemStack item) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long last = lastShotMillis.getOrDefault(id, 0L);

        if (now - last < fireRateCooldownMillis || player.hasCooldown(item.getType())) {
            return; // hard fire-rate cap, fail silently like Hypixel does
        }

        double cost;
        if (now - last > resetAfterMillis) {
            cost = baseCost;
        } else {
            cost = lastCost.getOrDefault(id, 0.0) + costIncrement;
        }

        ManaManager mana = plugin.getManaManager();
        if (!mana.has(player, cost)) {
            player.sendMessage(legacy("&cYou do not have enough Mana to use this ability!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        mana.remove(player, cost);
        lastShotMillis.put(id, now);
        lastCost.put(id, cost);

        // Visible + functional per-item cooldown matching the fire-rate cap,
        // so the hotbar slot actually shows the swirl like a real ability item.
        int cooldownTicks = (int) Math.max(1, fireRateCooldownMillis / 50L);
        player.setCooldown(item.getType(), cooldownTicks);

        fireBullet(player);
    }

    private void fireBullet(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = applySpread(eye.getDirection(), spreadDegrees);

        player.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.6f);
        // Subtle villager-ish flourish on top of the blaze shot, since Jerry
        // himself (and most Jerry-themed items) use the villager voice.
        player.getWorld().playSound(eye, Sound.ENTITY_VILLAGER_AMBIENT, 0.4f, 1.8f);

        Location origin = eye.clone().add(direction.clone().multiply(0.6));
        new JerrychineBullet(
                plugin,
                player,
                origin,
                direction,
                abilityDamage,
                bulletSpeedBlocksPerSecond,
                maxRange,
                knockbackRadius,
                knockbackUpward,
                knockbackHorizontal,
                allowWallPiercing,
                skullTexture
        ).start();
    }

    /**
     * Randomly perturbs a direction vector within a cone of the given angle
     * (Hypixel: 3 degree spread).
     */
    private Vector applySpread(Vector direction, double maxDegrees) {
        Vector dir = direction.clone().normalize();
        if (maxDegrees <= 0) {
            return dir;
        }

        Vector up = new Vector(0, 1, 0);
        Vector perpendicular1 = Math.abs(dir.dot(up)) > 0.999
                ? dir.clone().crossProduct(new Vector(1, 0, 0))
                : dir.clone().crossProduct(up);
        perpendicular1.normalize();
        Vector perpendicular2 = dir.clone().crossProduct(perpendicular1).normalize();

        double maxRad = Math.toRadians(maxDegrees);
        double randomAngle = Math.random() * Math.PI * 2;
        double randomMagnitude = Math.random() * maxRad;

        Vector offset = perpendicular1.multiply(Math.cos(randomAngle))
                .add(perpendicular2.multiply(Math.sin(randomAngle)))
                .multiply(Math.sin(randomMagnitude));

        return dir.clone().multiply(Math.cos(randomMagnitude)).add(offset).normalize();
    }
}
