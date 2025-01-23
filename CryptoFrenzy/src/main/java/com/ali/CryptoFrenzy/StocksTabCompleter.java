package com.ali.CryptoFrenzy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
        if (!(sender instanceof Player)) {
            return null;
        }

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("stocks.help.admin")) {
                List<String> subcommands = Arrays.asList("list", "buy", "sell", "fetch", "help", "portfolio", "send", "reload", "add", "remove","crash");
                for (String subcommand : subcommands) {
                    if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                        suggestions.add(subcommand);
                    }
                }
            } else {
                List<String> subcommands = Arrays.asList("list", "buy", "sell", "fetch", "help", "portfolio", "send");
                for (String subcommand : subcommands) {
                    if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                        suggestions.add(subcommand);
                    }
                }
            }
            return suggestions;
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            // Show available stocks
            if (subcommand.equalsIgnoreCase("buy") || subcommand.equalsIgnoreCase("sell") || subcommand.equalsIgnoreCase("fetch") || (sender.hasPermission("stocks.crash") && subcommand.equalsIgnoreCase("crash"))) {
                List<String> stockNames = new ArrayList<>(plugin.getPricesConfig().getConfigurationSection("Stocks").getKeys(false));
                for (String stock : stockNames) {
                    if (stock.toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(stock);
                    }
                }
                return suggestions;
            }
            if (sender.hasPermission("stocks.crash") && subcommand.equalsIgnoreCase("crash")) {
                if ("confirm".toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add("confirm");
                }
                return suggestions;
            }

            // Shows available players
            if (subcommand.equalsIgnoreCase("add") || subcommand.equalsIgnoreCase("remove") || subcommand.equalsIgnoreCase("send") || subcommand.equalsIgnoreCase("portfolio")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().startsWith(args[1])) {
                        suggestions.add(player.getName());
                    }
                }
                return suggestions;
            }
        }

        if (args.length == 3) {
            String subcommand = args[0].toLowerCase();

            // Shows numbers
            if (subcommand.equalsIgnoreCase("buy") || subcommand.equalsIgnoreCase("sell")) {
                List<String> amounts = Arrays.asList("1", "10", "100");
                for (String amount : amounts) {
                    if (amount.startsWith(args[2])) {
                        suggestions.add(amount);
                    }
                }
                return suggestions;
            }

            // Shows stocks
            if (subcommand.equalsIgnoreCase("add") || subcommand.equalsIgnoreCase("remove") || subcommand.equalsIgnoreCase("send")) {
                List<String> stockNames = new ArrayList<>(plugin.getPricesConfig().getConfigurationSection("Stocks").getKeys(false));
                for (String stock : stockNames) {
                    if (stock.toLowerCase().startsWith(args[2].toLowerCase())) {
                        suggestions.add(stock);
                    }
                }
                return suggestions;
            }
        }

        if (args.length == 4) {
            String subcommand = args[0].toLowerCase();

            // Shows numbers
            if (subcommand.equalsIgnoreCase("add") || subcommand.equalsIgnoreCase("remove") || subcommand.equalsIgnoreCase("send")) {
                List<String> amounts = Arrays.asList("1", "10", "100");
                for (String amount : amounts) {
                    if (amount.startsWith(args[3])) {
                        suggestions.add(amount);
                    }
                }
                return suggestions;
            }
        }
        return null;
    }
}