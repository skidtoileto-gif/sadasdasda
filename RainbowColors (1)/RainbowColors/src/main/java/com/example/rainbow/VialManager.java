package com.example.rainbow;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * "Vials of Essence": craftable once your ability is unlocked, drinkable by
 * another player (with an unlocked ability) to mix that color into their own.
 */
public class VialManager {

    public static final String RECIPE_PREFIX = "vial_";

    private final RainbowPlugin plugin;
    private final NamespacedKey vialKey;

    public VialManager(RainbowPlugin plugin) {
        this.plugin = plugin;
        this.vialKey = new NamespacedKey(plugin, "vial_color");
    }

    public ItemStack createVial(RainbowColor color) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setColor(color.rgb());
        meta.setDisplayName(color.chat() + "Vial of " + color.display() + " Essence");
        meta.setLore(List.of(
                ChatColor.GRAY + "Drink to mix " + color.chat() + color.display() + ChatColor.GRAY + " into your color.",
                ChatColor.GRAY + "Requires an unlocked ability.",
                ChatColor.DARK_GRAY + "Grants: " + color.passiveDesc() + " + " + color.abilityName()));
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        meta.getPersistentDataContainer().set(vialKey, PersistentDataType.STRING, color.name());
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the color stored in a vial, or null if the item is not a vial. */
    public RainbowColor readVial(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) return null;
        String name = item.getItemMeta().getPersistentDataContainer().get(vialKey, PersistentDataType.STRING);
        return RainbowColor.fromName(name);
    }

    public void registerRecipes() {
        for (RainbowColor color : RainbowColor.values()) {
            ShapelessRecipe recipe = new ShapelessRecipe(recipeKey(color), createVial(color));
            recipe.addIngredient(Material.GLASS_BOTTLE);
            recipe.addIngredient(color.dye());
            recipe.addIngredient(Material.GLOWSTONE_DUST);
            plugin.getServer().addRecipe(recipe);
        }
    }

    public void unregisterRecipes() {
        for (RainbowColor color : RainbowColor.values()) {
            plugin.getServer().removeRecipe(recipeKey(color));
        }
    }

    public NamespacedKey recipeKey(RainbowColor color) {
        return new NamespacedKey(plugin, RECIPE_PREFIX + color.name().toLowerCase());
    }

    public String recipeText(RainbowColor color) {
        return "Glass Bottle + " + pretty(color.dye()) + " + Glowstone Dust (shapeless)";
    }

    private String pretty(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
