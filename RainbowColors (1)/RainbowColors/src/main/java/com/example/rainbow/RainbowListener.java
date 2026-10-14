package com.example.rainbow;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;

public class RainbowListener implements Listener {

    private final RainbowPlugin plugin;

    public RainbowListener(RainbowPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------ spawning / joining

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null) {
            d = plugin.assignRandom(p);
            p.sendTitle(d.primary.chat() + "You are " + d.primary.display() + "!", d.primary.chat() + d.primary.passiveDesc(), 10, 80, 20);
            p.sendMessage(RainbowPlugin.PREFIX + "You have been assigned the color " + d.primary.colored() + ".");
            p.sendMessage(RainbowPlugin.PREFIX + "Passive: " + d.primary.chat() + d.primary.passiveDesc());
            p.sendMessage(RainbowPlugin.PREFIX + "Unlock " + d.primary.chat() + d.primary.abilityName() + ChatColor.RESET
                    + " by: " + d.primary.unlockDesc() + " (" + plugin.threshold(d.primary) + "). Type /color for details.");
        } else {
            p.sendMessage(RainbowPlugin.PREFIX + "Welcome back, " + d.primary.colored() + ". Type /color for details.");
            plugin.applyPassives(p);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (plugin.getConfig().getBoolean("reassign-on-respawn", false)) {
            PlayerData d = plugin.assignRandom(p);
            p.sendMessage(RainbowPlugin.PREFIX + "You respawned as " + d.primary.colored() + "!");
        } else {
            // Effects are cleared on death, so give passives back right after respawn.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.applyPassives(p), 5L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.data().save();
    }

    // ------------------------------------------------------- unlock progress

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer != null && e.getEntity() instanceof Enemy) {
            plugin.addProgress(killer, RainbowColor.RED, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) return;
        Block b = e.getBlock();

        if (isOre(b.getType())) {
            plugin.addProgress(p, RainbowColor.ORANGE, 1);
        }
        if (b.getBlockData() instanceof Ageable crop && crop.getAge() == crop.getMaximumAge()) {
            plugin.addProgress(p, RainbowColor.GREEN, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        Player p = e.getPlayer();
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null || d.unlocked || d.primary != RainbowColor.YELLOW) return;
        if (p.isInsideVehicle() || from.distanceSquared(to) > 25) return; // ignore rides and teleports

        plugin.addProgress(p, RainbowColor.YELLOW, 1);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.addProgress(e.getPlayer(), RainbowColor.BLUE, 1);
        }
    }

    @EventHandler
    public void onLevel(PlayerLevelChangeEvent e) {
        Player p = e.getPlayer();
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null || d.primary != RainbowColor.INDIGO || d.unlocked) return;
        plugin.addProgress(p, RainbowColor.INDIGO, e.getNewLevel() - d.progress);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        plugin.addProgress(e.getEnchanter(), RainbowColor.VIOLET, 1);
    }

    // ----------------------------------------------------------- passive extras

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d != null && d.has(RainbowColor.GREEN)) {
            e.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ ability

    /** Shift + F (swap hands while sneaking) casts the ability. */
    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null) return;
        e.setCancelled(true);
        plugin.abilities().activate(p);
    }

    // ----------------------------------------------------- crafting & mixing

    /** Only players whose own ability is unlocked can craft a vial of their own color. */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        Recipe recipe = e.getRecipe();
        if (!(recipe instanceof ShapelessRecipe shapeless)) return;
        if (!shapeless.getKey().getNamespace().equals(plugin.getName().toLowerCase())) return;
        if (!shapeless.getKey().getKey().startsWith(VialManager.RECIPE_PREFIX)) return;

        RainbowColor color = plugin.vials().readVial(shapeless.getResult());
        if (color == null) return;

        PlayerData d = plugin.data().get(e.getView().getPlayer().getUniqueId());
        if (d == null || !d.unlocked || d.primary != color) {
            e.getInventory().setResult(null);
        }
    }

    /** Drinking a vial mixes its color into the drinker's color. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrink(PlayerItemConsumeEvent e) {
        RainbowColor vial = plugin.vials().readVial(e.getItem());
        if (vial == null) return;

        Player p = e.getPlayer();
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null) {
            e.setCancelled(true);
            return;
        }
        if (!d.unlocked) {
            e.setCancelled(true);
            p.sendMessage(RainbowPlugin.PREFIX + "You must unlock your own ability before you can mix colors.");
            return;
        }
        if (vial == d.primary) {
            e.setCancelled(true);
            p.sendMessage(RainbowPlugin.PREFIX + "That is your own color. Give it to someone else!");
            return;
        }
        if (d.mixed.contains(vial)) {
            e.setCancelled(true);
            p.sendMessage(RainbowPlugin.PREFIX + "You already carry " + vial.colored() + ".");
            return;
        }
        if (d.mixed.size() >= plugin.maxMixes()) {
            e.setCancelled(true);
            p.sendMessage(RainbowPlugin.PREFIX + "You can't mix any more colors.");
            return;
        }

        // Valid mix: let the drink go through (bottle is returned) and apply the color.
        d.mixed.add(vial);
        String name = RainbowColor.mixName(d.primary, vial);
        org.bukkit.Color blended = d.primary.rgb().mixColors(vial.rgb());

        p.sendTitle(d.primary.chat() + "" + ChatColor.BOLD + name + "!", d.primary.colored() + " + " + vial.colored(), 10, 80, 20);
        p.sendMessage(RainbowPlugin.PREFIX + "You mixed " + d.primary.colored() + " and " + vial.colored()
                + " into " + ChatColor.BOLD + name + ChatColor.RESET + "!");
        p.sendMessage(RainbowPlugin.PREFIX + "You gained " + vial.chat() + vial.passiveDesc() + ChatColor.RESET
                + " and " + vial.chat() + vial.abilityName() + ChatColor.RESET + " (cast together with Shift + F).");

        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 150, 0.6, 1, 0.6, 0,
                new Particle.DustOptions(blended, 1.8f));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.3f);
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.applyPassives(p));
    }

    private boolean isOre(Material m) {
        return m.name().endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
    }
}
