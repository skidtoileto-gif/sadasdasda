package com.example.rainbow;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final RainbowPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> players = new HashMap<>();

    public DataManager(RainbowPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public PlayerData get(UUID id) {
        return players.get(id);
    }

    public void put(UUID id, PlayerData data) {
        players.put(id, data);
    }

    public void load() {
        players.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            RainbowColor primary = RainbowColor.fromName(sec.getString("color"));
            if (primary == null) continue;
            try {
                PlayerData data = new PlayerData(primary);
                data.progress = sec.getInt("progress");
                data.unlocked = sec.getBoolean("unlocked");
                for (String name : sec.getStringList("mixed")) {
                    RainbowColor mixed = RainbowColor.fromName(name);
                    if (mixed != null && mixed != primary && !data.mixed.contains(mixed)) {
                        data.mixed.add(mixed);
                    }
                }
                players.put(UUID.fromString(key), data);
            } catch (IllegalArgumentException ignored) {
                // bad UUID in file, skip it
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerData d = entry.getValue();
            yaml.set(path + ".color", d.primary.name());
            yaml.set(path + ".progress", d.progress);
            yaml.set(path + ".unlocked", d.unlocked);
            yaml.set(path + ".mixed", d.mixed.stream().map(Enum::name).toList());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save players.yml: " + ex.getMessage());
        }
    }
}
