package net.dungeonhd.jerryGunPlugin.ability;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.dungeonhd.jerryGunPlugin.JerryGunPlugin;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Simulates a single Jerry-chine Gun bullet tick by tick using manual ray
 * tracing. This gives full control over Hypixel-specific quirks that vanilla
 * projectiles don't support out of the box, namely piercing 1-block-thick
 * walls and knocking the *shooter* back on impact instead of the victim.
 */
public class JerrychineBullet extends BukkitRunnable {

    private final JerryGunPlugin plugin;
    private final Player shooter;
    private final Vector direction;

    private final double damage;
    private final double speedPerTick;
    private final double maxRange;
    private final double knockbackRadius;
    private final double knockbackUpward;
    private final double knockbackHorizontal;
    private final boolean allowWallPierce;
    private final String skullTexture;

    private Location current;
    private double traveled = 0.0;
    private boolean piercedOnce = false;
    private ItemDisplay cosmetic;

    public JerrychineBullet(JerryGunPlugin plugin, Player shooter, Location origin, Vector direction,
                             double damage, double bulletSpeedBlocksPerSecond, double maxRange,
                             double knockbackRadius, double knockbackUpward, double knockbackHorizontal,
                             boolean allowWallPierce, String skullTexture) {
        this.plugin = plugin;
        this.shooter = shooter;
        this.current = origin.clone();
        this.direction = direction.clone().normalize();
        this.damage = damage;
        this.speedPerTick = bulletSpeedBlocksPerSecond / 20.0;
        this.maxRange = maxRange;
        this.knockbackRadius = knockbackRadius;
        this.knockbackUpward = knockbackUpward;
        this.knockbackHorizontal = knockbackHorizontal;
        this.allowWallPierce = allowWallPierce;
        this.skullTexture = skullTexture;
    }

    /**
     * Spawns the cosmetic bullet entity and starts the per-tick simulation.
     */
    public void start() {
        spawnCosmetic();
        runTaskTimer(plugin, 1L, 1L);
    }

    private void spawnCosmetic() {
        World world = current.getWorld();
        if (world == null) {
            return;
        }
        cosmetic = world.spawn(current, ItemDisplay.class, display -> {
            display.setItemStack(buildBulletHead());
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setInvulnerable(true);
        });
    }

    /**
     * Builds the player-head ItemStack used as the bullet's cosmetic model.
     * If a Jerry skull texture (base64, see config.yml) is configured, the
     * bullet looks like an actual Jerry head as on Hypixel; otherwise it
     * falls back to a plain head so the plugin still works without one.
     */
    private ItemStack buildBulletHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (skullTexture == null || skullTexture.isBlank()) {
            return head;
        }
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "Jerry");
        profile.setProperty(new ProfileProperty("textures", skullTexture));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    @Override
    public void run() {
        World world = current.getWorld();
        if (world == null || !current.getChunk().isLoaded()) {
            finish(current, null);
            return;
        }

        Vector step = direction.clone().multiply(speedPerTick);
        double stepLength = step.length();

        RayTraceResult entityHit = world.rayTraceEntities(current, direction, stepLength, 0.4, this::isValidTarget);
        RayTraceResult blockHit = world.rayTraceBlocks(current, direction, stepLength, FluidCollisionMode.NEVER, true);

        if (entityHit != null && entityHit.getHitEntity() != null
                && (blockHit == null || closer(entityHit, blockHit))) {
            LivingEntity victim = (LivingEntity) entityHit.getHitEntity();
            finish(toLocation(world, entityHit.getHitPosition()), victim);
            return;
        }

        if (blockHit != null) {
            Block hitBlock = blockHit.getHitBlock();
            BlockFace face = blockHit.getHitBlockFace();
            if (allowWallPierce && !piercedOnce && hitBlock != null && face != null && isThinWall(hitBlock, face)) {
                piercedOnce = true;
                current = toLocation(world, blockHit.getHitPosition()).add(direction.clone().multiply(1.05));
                moveCosmetic();
                traveled += stepLength;
                if (traveled >= maxRange) {
                    finish(current, null);
                }
                return;
            }
            finish(toLocation(world, blockHit.getHitPosition()), null);
            return;
        }

        current = current.clone().add(step);
        moveCosmetic();
        traveled += stepLength;
        if (traveled >= maxRange) {
            finish(current, null);
        }
    }

    private boolean closer(RayTraceResult entityHit, RayTraceResult blockHit) {
        double entityDist = entityHit.getHitPosition().distanceSquared(current.toVector());
        double blockDist = blockHit.getHitPosition().distanceSquared(current.toVector());
        return entityDist <= blockDist;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (entity instanceof Player) {
            // "Shooting other players with the bullets has no effect" - let it pass through.
            return false;
        }
        return entity != shooter && !living.isDead() && !living.isInvulnerable();
    }

    private boolean isThinWall(Block hitBlock, BlockFace face) {
        Block beyond = hitBlock.getRelative(face.getOppositeFace());
        return !beyond.getType().isSolid();
    }

    private void finish(Location impactLocation, LivingEntity victim) {
        impact(impactLocation, victim);
        if (cosmetic != null && !cosmetic.isDead()) {
            cosmetic.remove();
        }
        cancel();
    }

    private void impact(Location loc, LivingEntity victim) {
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.CRIT, loc, 14, 0.2, 0.2, 0.2, 0.15);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
        }

        if (victim != null) {
            victim.damage(damage, shooter);
        }

        double distanceToShooter = loc.distance(shooter.getLocation());
        if (distanceToShooter <= knockbackRadius) {
            Vector away = shooter.getLocation().toVector().subtract(loc.toVector());
            away.setY(0);
            if (away.lengthSquared() < 1.0E-4) {
                away = shooter.getEyeLocation().getDirection().multiply(-1).setY(0);
            }
            if (away.lengthSquared() < 1.0E-4) {
                away = new Vector(1, 0, 0);
            }
            away.normalize();

            Vector knockback = away.multiply(knockbackHorizontal);
            knockback.setY(knockbackUpward);
            shooter.setVelocity(shooter.getVelocity().add(knockback));
        }
    }

    private void moveCosmetic() {
        if (cosmetic != null && !cosmetic.isDead()) {
            cosmetic.teleport(current);
        }
    }

    private Location toLocation(World world, Vector vector) {
        return new Location(world, vector.getX(), vector.getY(), vector.getZ());
    }
}
