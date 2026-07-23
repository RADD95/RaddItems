package me.radd.customitems.commands;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.sets.ItemSetDefinition;
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

public class RaddItemsCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERMISSION = "radditems.admin";

    private final RaddItemsPlugin plugin;

    public RaddItemsCommand(RaddItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(message("players-only"));
                return true;
            }

            if (args.length < 2) {
                sendUsage(sender);
                return true;
            }

            if (args[1].equalsIgnoreCase("set")) {
                if (args.length < 3) {
                    sender.sendMessage(message("usage-give-set"));
                    return true;
                }

                String setId = args[2];
                ItemSetDefinition set = plugin.getSetRegistry().get(setId);
                if (set == null) {
                    sender.sendMessage(message("set-not-found", "%id%", setId));
                    return true;
                }

                List<String> missingItems = new ArrayList<>();
                int givenAmount = 0;

                for (String pieceId : set.getPieces()) {
                    ItemStack item = plugin.getItemFactory().createItem(pieceId);
                    if (item == null) {
                        missingItems.add(pieceId);
                        continue;
                    }

                    p.getInventory().addItem(item);
                    givenAmount++;
                }

                if (givenAmount > 0) {
                    sender.sendMessage(message(
                            "set-given",
                            new String[]{"%id%", "%amount%"},
                            new String[]{setId, String.valueOf(givenAmount)}
                    ));
                } else {
                    sender.sendMessage(message("set-no-items-created", "%id%", setId));
                }

                if (!missingItems.isEmpty()) {
                    sender.sendMessage(message("set-missing-items", "%items%", String.join(", ", missingItems)));
                }

                return true;
            }

            String id = args[1];
            ItemStack item = plugin.getItemFactory().createItem(id);
            if (item == null) {
                sender.sendMessage(message("item-not-found", "%id%", id));
                return true;
            }

            p.getInventory().addItem(item);
            sender.sendMessage(message("item-given", "%id%", id));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginData();
            sender.sendMessage(message("reload-success"));
            return true;
        }

        sender.sendMessage(message("unknown-subcommand"));
        sendUsage(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return suggestions;
        }

        if (args.length == 1) {
            addIfMatches(suggestions, args[0], "give");
            addIfMatches(suggestions, args[0], "reload");
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            addIfMatches(suggestions, args[1], "set");

            for (String id : plugin.getItemRegistry().getIds()) {
                addIfMatches(suggestions, args[1], id);
            }

            return suggestions;
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("give")
                && args[1].equalsIgnoreCase("set")) {

            for (String id : plugin.getSetRegistry().getIds()) {
                addIfMatches(suggestions, args[2], id);
            }

            return suggestions;
        }

        return suggestions;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(message("usage-give-item"));
        sender.sendMessage(message("usage-give-set"));
        sender.sendMessage(message("usage-reload"));
    }

    private void addIfMatches(List<String> list, String input, String value) {
        if (input == null || input.isEmpty()) {
            list.add(value);
            return;
        }

        if (value.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT))) {
            list.add(value);
        }
    }

    private String message(String path) {
        String prefix = plugin.getConfig().getString("prefix", "");
        String text = plugin.getConfig().getString("messages." + path, "&cMissing message: " + path);
        return color(prefix + text);
    }

    private String message(String path, String placeholder, String value) {
        String prefix = plugin.getConfig().getString("prefix", "");
        String text = plugin.getConfig().getString("messages." + path, "&cMissing message: " + path);
        text = text.replace(placeholder, value);
        return color(prefix + text);
    }

    private String message(String path, String[] placeholders, String[] values) {
        String prefix = plugin.getConfig().getString("prefix", "");
        String text = plugin.getConfig().getString("messages." + path, "&cMissing message: " + path);

        int limit = Math.min(placeholders.length, values.length);
        for (int i = 0; i < limit; i++) {
            text = text.replace(placeholders[i], values[i]);
        }

        return color(prefix + text);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}