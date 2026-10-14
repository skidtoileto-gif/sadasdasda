package com.example.rainbow;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ColorCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "rainbow.admin";
    private final RainbowPlugin plugin;

    public ColorCommand(RainbowPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "info" -> {
                if (requirePlayer(sender)) info((Player) sender);
            }
            case "ability", "cast" -> {
                if (requirePlayer(sender)) plugin.abilities().activate((Player) sender);
            }
            case "recipe" -> {
                if (requirePlayer(sender)) recipe((Player) sender);
            }
            case "set", "unlock", "reset", "vial" -> admin(sender, sub, args);
            default -> sender.sendMessage(RainbowPlugin.PREFIX + "Usage: /color [info|ability|recipe]");
        }
        return true;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) return true;
        sender.sendMessage("This command can only be used by a player.");
        return false;
    }

    private void info(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null) {
            p.sendMessage(RainbowPlugin.PREFIX + "You have no color yet. Rejoin the server.");
            return;
        }
        RainbowColor c = d.primary;
        int need = plugin.threshold(c);

        p.sendMessage(ChatColor.DARK_GRAY + "------ " + c.colored() + ChatColor.GRAY + " ------");
        p.sendMessage(ChatColor.GRAY + "Passive: " + c.chat() + c.passiveDesc());
        p.sendMessage(ChatColor.GRAY + "Ability: " + c.chat() + c.abilityName() + ChatColor.GRAY + " - " + c.abilityDesc());
        if (d.unlocked) {
            p.sendMessage(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Unlocked" + ChatColor.GRAY
                    + " (Shift + F or /color ability)");
        } else {
            p.sendMessage(ChatColor.GRAY + "Unlock: " + c.chat() + c.unlockDesc() + ChatColor.GRAY + " "
                    + bar(d.progress, need, c) + ChatColor.GRAY + " " + d.progress + "/" + need);
        }

        if (d.mixed.isEmpty()) {
            p.sendMessage(ChatColor.GRAY + "Mixed colors: " + ChatColor.DARK_GRAY + "none");
        } else {
            for (RainbowColor m : d.mixed) {
                p.sendMessage(ChatColor.GRAY + "Mixed: " + m.colored() + ChatColor.GRAY + " -> "
                        + ChatColor.BOLD + RainbowColor.mixName(c, m) + ChatColor.RESET + ChatColor.GRAY
                        + " (" + m.passiveDesc() + ", " + m.abilityName() + ")");
            }
        }
        p.sendMessage(ChatColor.GRAY + "Mixing slots: " + d.mixed.size() + "/" + plugin.maxMixes());
    }

    private void recipe(Player p) {
        PlayerData d = plugin.data().get(p.getUniqueId());
        if (d == null) return;
        RainbowColor c = d.primary;
        p.sendMessage(RainbowPlugin.PREFIX + "Vial of " + c.colored() + " Essence: "
                + ChatColor.WHITE + plugin.vials().recipeText(c));
        p.sendMessage(ChatColor.GRAY + "You can only craft this once your ability is unlocked"
                + (d.unlocked ? ChatColor.GREEN + " (you can!)" : "") + ChatColor.GRAY + ".");
        p.sendMessage(ChatColor.GRAY + "Another player with an unlocked ability drinks it to mix your color into theirs.");
    }

    private String bar(int value, int max, RainbowColor c) {
        int filled = (int) Math.round(10.0 * value / max);
        StringBuilder sb = new StringBuilder();
        sb.append(c.chat());
        for (int i = 0; i < 10; i++) {
            if (i == filled) sb.append(ChatColor.DARK_GRAY);
            sb.append('|');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------- admin

    private void admin(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return;
        }

        if (sub.equals("vial")) {
            if (args.length < 2) {
                sender.sendMessage(RainbowPlugin.PREFIX + "Usage: /color vial <color> [player]");
                return;
            }
            RainbowColor color = RainbowColor.fromName(args[1]);
            Player target = args.length >= 3 ? plugin.getServer().getPlayerExact(args[2])
                    : (sender instanceof Player ps ? ps : null);
            if (color == null || target == null) {
                sender.sendMessage(RainbowPlugin.PREFIX + "Unknown color or player.");
                return;
            }
            ItemStack vial = plugin.vials().createVial(color);
            target.getInventory().addItem(vial).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
            sender.sendMessage(RainbowPlugin.PREFIX + "Gave " + target.getName() + " a vial of " + color.colored() + ".");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(RainbowPlugin.PREFIX + "Usage: /color " + sub + " <player>" + (sub.equals("set") ? " <color>" : ""));
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(RainbowPlugin.PREFIX + "Player not found.");
            return;
        }

        switch (sub) {
            case "set" -> {
                RainbowColor color = args.length >= 3 ? RainbowColor.fromName(args[2]) : null;
                if (color == null) {
                    sender.sendMessage(RainbowPlugin.PREFIX + "Unknown color.");
                    return;
                }
                plugin.setColor(target, color);
                target.sendMessage(RainbowPlugin.PREFIX + "Your color is now " + color.colored() + ".");
                sender.sendMessage(RainbowPlugin.PREFIX + target.getName() + " is now " + color.colored() + ".");
            }
            case "unlock" -> {
                plugin.unlockNow(target);
                sender.sendMessage(RainbowPlugin.PREFIX + "Unlocked " + target.getName() + "'s ability.");
            }
            case "reset" -> {
                PlayerData d = plugin.assignRandom(target);
                target.sendMessage(RainbowPlugin.PREFIX + "Your color was reset. You are now " + d.primary.colored() + ".");
                sender.sendMessage(RainbowPlugin.PREFIX + target.getName() + " is now " + d.primary.colored() + ".");
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------- tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission(ADMIN);

        if (args.length == 1) {
            out.addAll(List.of("info", "ability", "recipe"));
            if (admin) out.addAll(List.of("set", "unlock", "reset", "vial"));
        } else if (args.length == 2 && admin) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set", "unlock", "reset" -> plugin.getServer().getOnlinePlayers().forEach(p -> out.add(p.getName()));
                case "vial" -> addColors(out);
                default -> { }
            }
        } else if (args.length == 3 && admin) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> addColors(out);
                case "vial" -> plugin.getServer().getOnlinePlayers().forEach(p -> out.add(p.getName()));
                default -> { }
            }
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }

    private void addColors(List<String> out) {
        for (RainbowColor c : RainbowColor.values()) out.add(c.name().toLowerCase(Locale.ROOT));
    }
}
