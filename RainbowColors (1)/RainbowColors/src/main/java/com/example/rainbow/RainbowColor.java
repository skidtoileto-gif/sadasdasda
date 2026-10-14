package com.example.rainbow;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

/**
 * The seven colors of the rainbow, each with a passive power (permanent potion effects),
 * an ability and the requirement that unlocks it.
 */
public enum RainbowColor {

    RED("Red", ChatColor.RED, Color.fromRGB(255, 40, 40), Material.RED_DYE,
            "Inferno", "Ignite and blast every enemy around you.",
            "Kill hostile mobs", 25, 30,
            "Fire Resistance",
            new Passive(PotionEffectType.FIRE_RESISTANCE, 0)),

    ORANGE("Orange", ChatColor.GOLD, Color.fromRGB(255, 140, 0), Material.ORANGE_DYE,
            "Sunburst", "Haste III and Strength I for 20 seconds.",
            "Mine ores", 40, 60,
            "Haste I",
            new Passive(PotionEffectType.HASTE, 0)),

    YELLOW("Yellow", ChatColor.YELLOW, Color.fromRGB(255, 235, 0), Material.YELLOW_DYE,
            "Lightning Strike", "Call lightning down where you are looking.",
            "Travel blocks on foot", 3000, 20,
            "Speed I",
            new Passive(PotionEffectType.SPEED, 0)),

    GREEN("Green", ChatColor.GREEN, Color.fromRGB(40, 200, 60), Material.GREEN_DYE,
            "Bloom", "Heal nearby players and grow nearby crops.",
            "Harvest fully grown crops", 150, 45,
            "Jump Boost I + no fall damage",
            new Passive(PotionEffectType.JUMP_BOOST, 0)),

    BLUE("Blue", ChatColor.AQUA, Color.fromRGB(30, 120, 255), Material.LIGHT_BLUE_DYE,
            "Tidal Surge", "Blast nearby enemies away with a wave of water.",
            "Catch fish", 15, 25,
            "Water Breathing + Dolphin's Grace",
            new Passive(PotionEffectType.WATER_BREATHING, 0),
            new Passive(PotionEffectType.DOLPHINS_GRACE, 0)),

    INDIGO("Indigo", ChatColor.DARK_BLUE, Color.fromRGB(75, 0, 130), Material.BLUE_DYE,
            "Blink", "Teleport up to 12 blocks in the direction you are looking.",
            "Reach experience level", 20, 8,
            "Night Vision",
            new Passive(PotionEffectType.NIGHT_VISION, 0)),

    VIOLET("Violet", ChatColor.DARK_PURPLE, Color.fromRGB(150, 0, 210), Material.PURPLE_DYE,
            "Void Pulse", "Lift nearby enemies into the air and shield yourself.",
            "Enchant items", 5, 40,
            "+2 extra hearts",
            new Passive(PotionEffectType.HEALTH_BOOST, 0));

    /** A permanent potion effect granted by a color. */
    public record Passive(PotionEffectType type, int amplifier) {}

    private final String display;
    private final ChatColor chat;
    private final Color rgb;
    private final Material dye;
    private final String abilityName;
    private final String abilityDesc;
    private final String unlockDesc;
    private final int defaultThreshold;
    private final int defaultCooldown;
    private final String passiveDesc;
    private final Passive[] passives;

    RainbowColor(String display, ChatColor chat, Color rgb, Material dye,
                 String abilityName, String abilityDesc,
                 String unlockDesc, int defaultThreshold, int defaultCooldown,
                 String passiveDesc, Passive... passives) {
        this.display = display;
        this.chat = chat;
        this.rgb = rgb;
        this.dye = dye;
        this.abilityName = abilityName;
        this.abilityDesc = abilityDesc;
        this.unlockDesc = unlockDesc;
        this.defaultThreshold = defaultThreshold;
        this.defaultCooldown = defaultCooldown;
        this.passiveDesc = passiveDesc;
        this.passives = passives;
    }

    public String display() { return display; }
    public ChatColor chat() { return chat; }
    public Color rgb() { return rgb; }
    public Material dye() { return dye; }
    public String abilityName() { return abilityName; }
    public String abilityDesc() { return abilityDesc; }
    public String unlockDesc() { return unlockDesc; }
    public int defaultThreshold() { return defaultThreshold; }
    public int defaultCooldown() { return defaultCooldown; }
    public String passiveDesc() { return passiveDesc; }
    public Passive[] passives() { return passives; }

    /** Display name with its chat color applied, e.g. "§cRed". */
    public String colored() { return chat + display + ChatColor.RESET; }

    public static RainbowColor fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Cosmetic name for a mix of two colors. */
    public static String mixName(RainbowColor a, RainbowColor b) {
        RainbowColor lo = a.ordinal() < b.ordinal() ? a : b;
        RainbowColor hi = lo == a ? b : a;
        String key = lo.name() + "+" + hi.name();
        return switch (key) {
            case "RED+ORANGE" -> "Scarlet";
            case "RED+YELLOW" -> "Amber";
            case "RED+GREEN" -> "Rust";
            case "RED+BLUE" -> "Burgundy";
            case "RED+INDIGO" -> "Maroon";
            case "RED+VIOLET" -> "Magenta";
            case "ORANGE+YELLOW" -> "Gold";
            case "ORANGE+GREEN" -> "Olive";
            case "ORANGE+BLUE" -> "Bronze";
            case "ORANGE+INDIGO" -> "Sienna";
            case "ORANGE+VIOLET" -> "Coral";
            case "YELLOW+GREEN" -> "Lime";
            case "YELLOW+BLUE" -> "Mint";
            case "YELLOW+INDIGO" -> "Khaki";
            case "YELLOW+VIOLET" -> "Peach";
            case "GREEN+BLUE" -> "Turquoise";
            case "GREEN+INDIGO" -> "Forest";
            case "GREEN+VIOLET" -> "Moss";
            case "BLUE+INDIGO" -> "Sapphire";
            case "BLUE+VIOLET" -> "Lavender";
            case "INDIGO+VIOLET" -> "Plum";
            default -> lo.display + "-" + hi.display;
        };
    }
}
