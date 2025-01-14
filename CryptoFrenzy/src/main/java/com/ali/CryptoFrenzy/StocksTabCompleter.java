package com.ali.CryptoFrenzy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StocksTabCompleter implements TabCompleter {

    private CryptoFrenzy plugin;

    public StocksTabCompleter(CryptoFrenzy plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            return null;
        }

        List<String> suggestions = new ArrayList<>();

        // First argument: Suggest subcommands
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("list", "buy", "sell", "fetch", "reload", "help", "portfolio", "send", "delete");
            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(subcommand);
                }
            }
            return suggestions;
        }

        // Second argument: Suggest stock names for "buy", "sell", or "fetch" subcommands
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("buy") || subcommand.equals("sell") || subcommand.equals("fetch")) {
                List<String> stockNames = new ArrayList<>(plugin.getPricesConfig().getConfigurationSection("Stocks").getKeys(false));

                for (String stock : stockNames) {
                    if (stock.toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(stock);
                    }
                }
            }
            return suggestions;
        }

        // Third argument: Suggest numbers for "send" or "delete" subcommands
        if (args.length == 3 && (args[0].equalsIgnoreCase("send") || args[0].equalsIgnoreCase("delete"))) {
            List<String> amounts = Arrays.asList("1", "10", "100"); // Possible amounts for sending/deleting
            for (String amount : amounts) {
                if (amount.startsWith(args[2])) {
                    suggestions.add(amount);
                }
            }
            return suggestions;
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("sell"))) {
            List<String> amounts = Arrays.asList("1", "10", "100"); // Possible amounts for sending/deleting
            for (String amount : amounts) {
                if (amount.startsWith(args[2])) {
                    suggestions.add(amount);
                }
            }
            return suggestions;
        }

        return null; // Return null if there are no suggestions to offer
    }
}
