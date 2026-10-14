package com.example.rainbow;

import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {

    private final RainbowPlugin plugin;
    private final Map<UUID, Map<RainbowColor, Long>> cooldowns = new HashMap<>();

    public AbilityManager(RainbowPlugin plugin) {
        this.plugin = plugin;
    }

    public void clearCooldowns(UUID id) {
        cooldowns.remove(id);
    }

    /**
     * Casts the player's primary ability and the ability of every mixed color.
     */
    public void activate(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null) return;

        if (!d.unlocked) {
            int need = plugin.threshold(d.primary);
            p.sendMessage(RainbowPlugin.PREFIX + "Your ability is still locked. " + d.primary.chat()
                    + d.primary.unlockDesc() + ChatColor.RESET + ": " + d.progress + "/" + need);
            return;
        }

        Map<RainbowColor, Long> cd = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long now = System.currentTimeMillis();
        boolean castAny = false;

        for (RainbowColor color : d.all()) {
            long ready = cd.getOrDefault(color, 0L);
            if (now < ready) {
                long secs = (ready - now + 999) / 1000;
                p.sendMessage(RainbowPlugin.PREFIX + color.chat() + color.abilityName() + ChatColor.RESET
                        + " is on cooldown (" + secs + "s).");
                continue;
            }
            if (cast(p, color)) {
                cd.put(color, now + plugin.cooldownSeconds(color) * 1000L);
                castAny = true;
            }
        }

        if (castAny) {
            plugin.actionBar(p, d.primary.chat() + "" + ChatColor.BOLD + "Ability cast!");
        }
    }

    private boolean cast(Player p, RainbowColor color) {
        return switch (color) {
            case RED -> inferno(p);
            case ORANGE -> sunburst(p);
            case YELLOW -> lightning(p);
            case GREEN -> bloom(p);
            case BLUE -> tidalSurge(p);
            case INDIGO -> blink(p);
            case VIOLET -> voidPulse(p);
        };
    }

    // ---------------------------------------------------------------- abilities

    private boolean inferno(Player p) {
        for (Entity e : p.getNearbyEntities(6, 3, 6)) {
            if (!canTarget(p, e)) continue;
            LivingEntity target = (LivingEntity) e;
            target.setFireTicks(120);
            target.damage(4, p);
        }
        World w = p.getWorld();
        w.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 150, 3, 0.6, 3, 0.05);
        w.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f);
        return true;
    }

    private boolean sunburst(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 400, 2, false, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 0, false, true, true));
        World w = p.getWorld();
        dust(w, p.getLocation().add(0, 1, 0), RainbowColor.ORANGE, 80, 0.6);
        w.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.02);
        w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.6f);
        return true;
    }

    private boolean lightning(Player p) {
        Location eye = p.getEyeLocation();
        RayTraceResult hit = p.getWorld().rayTrace(eye, eye.getDirection(), 40,
                FluidCollisionMode.NEVER, true, 0.5, e -> e != p);
        if (hit == null) {
            p.sendMessage(RainbowPlugin.PREFIX + "No target in range.");
            return false;
        }
        Location target = hit.getHitPosition().toLocation(p.getWorld());
        p.getWorld().strikeLightning(target);
        p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target, 40, 0.8, 0.8, 0.8, 0.2);
        return true;
    }

    private boolean bloom(Player p) {
        Location center = p.getLocation();
        for (Player other : p.getWorld().getPlayers()) {
            if (other.getLocation().distanceSquared(center) <= 64) {
                other.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, true, true));
            }
        }

        Block origin = center.getBlock();
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    Block b = origin.getRelative(x, y, z);
                    if (b.getBlockData() instanceof Ageable crop && crop.getAge() < crop.getMaximumAge()) {
                        b.applyBoneMeal(BlockFace.UP);
                    }
                }
            }
        }
        World w = p.getWorld();
        w.spawnParticle(Particle.HAPPY_VILLAGER, center.clone().add(0, 1, 0), 60, 4, 1, 4, 0);
        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1f);
        return true;
    }

    private boolean tidalSurge(Player p) {
        Location center = p.getLocation();
        for (Entity e : p.getNearbyEntities(7, 3, 7)) {
            if (!canTarget(p, e)) continue;
            Vector push = e.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.01) push = new Vector(0.1, 0, 0);
            push.normalize().multiply(1.6).setY(0.6);
            e.setVelocity(push);
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true, true));
        }
        World w = p.getWorld();
        w.spawnParticle(Particle.SPLASH, center.clone().add(0, 1, 0), 250, 3.5, 0.8, 3.5, 0.3);
        w.playSound(center, Sound.ENTITY_PLAYER_SPLASH, 1.5f, 0.6f);
        return true;
    }

    private boolean blink(Player p) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();

        RayTraceResult hit = w.rayTraceBlocks(eye, dir, 12, FluidCollisionMode.NEVER, true);
        double dist = hit == null ? 12 : Math.max(0, hit.getHitPosition().distance(eye.toVector()) - 0.8);

        Location dest = p.getLocation().add(dir.clone().multiply(dist));
        for (int i = 0; i < 24 && !isSafe(dest); i++) {
            dest.subtract(dir.clone().multiply(0.5));
        }
        if (!isSafe(dest)) {
            p.sendMessage(RainbowPlugin.PREFIX + "No safe place to blink to.");
            return false;
        }

        dust(w, p.getLocation().add(0, 1, 0), RainbowColor.INDIGO, 60, 0.4);
        w.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.5);
        p.teleport(dest);
        p.setFallDistance(0);
        dust(w, dest.clone().add(0, 1, 0), RainbowColor.INDIGO, 60, 0.4);
        w.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        return true;
    }

    private boolean voidPulse(Player p) {
        for (Entity e : p.getNearbyEntities(8, 4, 8)) {
            if (!canTarget(p, e)) continue;
            LivingEntity target = (LivingEntity) e;
            target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 0, false, false, false));
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1, false, true, true));
        World w = p.getWorld();
        w.spawnParticle(Particle.WITCH, p.getLocation().add(0, 1, 0), 120, 3, 1, 3, 0.1);
        dust(w, p.getLocation().add(0, 1, 0), RainbowColor.VIOLET, 100, 3);
        w.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.4f);
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private boolean canTarget(Player caster, Entity e) {
        if (!(e instanceof LivingEntity) || e instanceof ArmorStand || e.equals(caster)) return false;
        if (e instanceof Tameable t && t.isTamed() && caster.equals(t.getOwner())) return false;
        return true;
    }

    private boolean isSafe(Location loc) {
        return loc.getBlock().isPassable() && loc.clone().add(0, 1, 0).getBlock().isPassable();
    }

    private void dust(World w, Location loc, RainbowColor color, int count, double spread) {
        w.spawnParticle(Particle.DUST, loc, count, spread, spread, spread, 0,
                new Particle.DustOptions(color.rgb(), 1.5f));
    }
}
