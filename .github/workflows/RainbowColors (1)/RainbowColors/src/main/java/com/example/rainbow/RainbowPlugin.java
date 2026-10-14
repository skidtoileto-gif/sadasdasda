package com.example.rainbow;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class RainbowPlugin extends JavaPlugin {

    public static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE + "Rainbow"
            + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    private DataManager data;
    private VialManager vials;
    private AbilityManager abilities;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        data = new DataManager(this);
        data.load();
        vials = new VialManager(this);
        vials.registerRecipes();
        abilities = new AbilityManager(this);

        getServer().getPluginManager().registerEvents(new RainbowListener(this), this);

        ColorCommand command = new ColorCommand(this);
        PluginCommand pc = getCommand("color");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        // Refresh passive effects every 5 seconds (effects last 15s so they never flicker).
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) applyPassives(p);
        }, 20L, 100L);

        // Autosave every 5 minutes.
        getServer().getScheduler().runTaskTimer(this, () -> data.save(), 6000L, 6000L);

        // Handle /reload: give a color to anyone online who has none.
        for (Player p : getServer().getOnlinePlayers()) {
            if (data.get(p.getUniqueId()) == null) assignRandom(p);
        }
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
        if (vials != null) vials.unregisterRecipes();
    }

    public DataManager data() { return data; }
    public VialManager vials() { return vials; }
    public AbilityManager abilities() { return abilities; }

    // ------------------------------------------------------------------ colors

    public PlayerData assignRandom(Player p) {
        RainbowColor[] all = RainbowColor.values();
        RainbowColor color = all[ThreadLocalRandom.current().nextInt(all.length)];
        return setColor(p, color);
    }

    /** Sets a player's primary color and resets progress, unlock state and mixes. */
    public PlayerData setColor(Player p, RainbowColor color) {
        UUID id = p.getUniqueId();
        PlayerData old = data.get(id);
        if (old != null) clearPassives(p, old);

        PlayerData fresh = new PlayerData(color);
        data.put(id, fresh);
        abilities.clearCooldowns(id);
        applyPassives(p);
        return fresh;
    }

    public void applyPassives(Player p) {
        PlayerData d = data.get(p.getUniqueId());
        if (d == null || p.getGameMode() == GameMode.SPECTATOR) return;

        for (RainbowColor color : d.all()) {
            for (RainbowColor.Passive passive : color.passives()) {
                PotionEffect current = p.getPotionEffect(passive.type());
                // Don't downgrade a stronger effect (e.g. an ability buff or a real potion).
                if (current != null && (current.getAmplifier() > passive.amplifier()
                        || (current.getAmplifier() == passive.amplifier() && current.getDuration() > 300))) {
                    continue;
                }
                p.addPotionEffect(new PotionEffect(passive.type(), 300, passive.amplifier(), true, false, true));
            }
        }
    }

    private void clearPassives(Player p, PlayerData d) {
        for (RainbowColor color : d.all()) {
            for (RainbowColor.Passive passive : color.passives()) {
                p.removePotionEffect(passive.type());
            }
        }
    }

    // ---------------------------------------------------------------- progress

    public int threshold(RainbowColor color) {
        return Math.max(1, getConfig().getInt("unlock-requirements." + color.name(), color.defaultThreshold()));
    }

    public int cooldownSeconds(RainbowColor color) {
        return Math.max(0, getConfig().getInt("cooldowns." + color.name(), color.defaultCooldown()));
    }

    public int maxMixes() {
        return Math.max(0, getConfig().getInt("max-mixes", 1));
    }

    /** Adds progress towards unlocking the player's ability, if the color matches. */
    public void addProgress(Player p, RainbowColor color, int amount) {
        PlayerData d = data.get(p.getUniqueId());
        if (d == null || d.unlocked || d.primary != color || amount <= 0) return;

        int need = threshold(color);
        d.progress = Math.min(need, d.progress + amount);

        if (d.progress >= need) {
            d.unlocked = true;
            p.sendTitle(color.chat() + "Ability Unlocked!", color.chat() + color.abilityName(), 10, 70, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.sendMessage(PREFIX + "You unlocked " + color.chat() + color.abilityName() + ChatColor.RESET
                    + "! Press " + ChatColor.YELLOW + "Shift + F" + ChatColor.RESET + " or use "
                    + ChatColor.YELLOW + "/color ability" + ChatColor.RESET + " to cast it.");
            p.sendMessage(PREFIX + "You can now craft a " + color.chat() + "Vial of " + color.display()
                    + " Essence" + ChatColor.RESET + " (/color recipe). Give it to someone else so they can mix it!");
            return;
        }

        int step = Math.max(1, need / 20);
        if (d.progress % step == 0) {
            actionBar(p, color.chat() + color.unlockDesc() + ": " + d.progress + "/" + need);
        }
    }

    public void unlockNow(Player p) {
        PlayerData d = data.get(p.getUniqueId());
        if (d == null) return;
        d.progress = threshold(d.primary);
        d.unlocked = false; // ensure the normal unlock flow fires
        addProgress(p, d.primary, 1);
    }

    public void actionBar(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}
