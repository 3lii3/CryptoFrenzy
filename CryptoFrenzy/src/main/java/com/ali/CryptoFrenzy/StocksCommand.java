package com.ali.CryptoFrenzy;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;

import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.logging.Logger;

public class StocksCommand implements CommandExecutor {

    private CryptoFrenzy plugin;
    PricingHandler pricingHandler;
    private FileConfiguration pricesConfig;
    private StockHistory stockHistory;

    private Map<Player, Long> lastCommandTime = new HashMap<>();
    private Map<Player, String> pendingCrash = new HashMap<>();


    public StocksCommand(CryptoFrenzy plugin, StockHistory stockHistory) {
        this.plugin = plugin;
        this.pricingHandler = new PricingHandler(plugin);
        this.pricesConfig = plugin.getPricesConfig();
        this.stockHistory = stockHistory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length > 0 && args[0].toString().equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                pricingHandler.Reload();
                stockHistory.CheckForNewCurrencies();
                sender.sendMessage(ChatColor.GREEN + "All plugin configurations have been re-loaded.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED +"This command can only be run by a player.");
                return false;
            }
        }

        Player player = (Player) sender;

        // Check if cooldown is enabled in config
        boolean cooldownEnabled = plugin.getConfig().getBoolean("player.cooldown.enabled");
        boolean hasCooldownTag = player.hasPermission("stocks.cooldown");

        // Apply cooldown only for buy/sell commands and if the player doesn't have stocks.cooldown tag
        if (cooldownEnabled && !hasCooldownTag && (command.getName().equalsIgnoreCase("stock") && (args.length > 0 && (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("sell"))))) {
            int cooldownTime = plugin.getConfig().getInt("player.cooldown.time") * 1000; // Convert secs to ms
            long currentTime = System.currentTimeMillis();
            long lastTime = lastCommandTime.getOrDefault(player, 0L);

            if (currentTime - lastTime < cooldownTime) {
                long timeRemaining = (cooldownTime - (currentTime - lastTime)) / 1000; // convert ms to secs again.
                player.sendMessage(ChatColor.RED + "You must wait " + timeRemaining + " seconds before using this command again.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                return false;
            }
            lastCommandTime.put(player, currentTime);
        }

        // Process the command
        switch (command.getName().toLowerCase()) {
            case "stocks":
                return handleStocksCommand(player, args);
            case "cryptofrenzy":
                return handleCryptoFrenzyCommand(player);
            default:
                player.sendMessage(ChatColor.RED + "Unknown command.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                return false;
        }
    }

    private boolean handleStocksCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Unknown subcommand. Type " + ChatColor.YELLOW + "/stocks help" + ChatColor.RED + " for available commands.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                if (!player.hasPermission("stocks.list")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                listStocks(player);
                break;

            case "buy":
                if (!player.hasPermission("stocks.buy")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks buy <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[1].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }

                try {
                    int amount = Integer.parseInt(args[2]);
                    buyStock(player, args[1].toUpperCase(), amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount! Please enter a valid number.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                break;

            case "sell":
                if (!player.hasPermission("stocks.sell")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks sell <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[1].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                sellStock(player, args[1].toUpperCase(), Integer.parseInt(args[2]));
                break;

            case "remove":
                if (!player.hasPermission("stocks.remove")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks remove <Player> <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[2].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (!Bukkit.getOfflinePlayer(args[1].toString()).hasPlayedBefore()) {
                    player.sendMessage(ChatColor.RED+"This player has never played before.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }

                removeStockFromPlayer(Bukkit.getOfflinePlayer(args[1].toString()), args[2].toUpperCase(),Integer.parseInt(args[3]), player);
                break;

            case "add":
                if (!player.hasPermission("stocks.add")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks add <Player> <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[2].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (!Bukkit.getOfflinePlayer(args[1].toString()).hasPlayedBefore()) {
                    player.sendMessage(ChatColor.RED+"This player has never played before.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }

                addStockToPlayer(Bukkit.getOfflinePlayer(args[1].toString()), args[2].toUpperCase(),Integer.parseInt(args[3]), player);
                break;

            case "send":
                if (!player.hasPermission("stocks.send")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks send <Player> <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args[1].toString() == player.getName()) {
                    player.sendMessage(ChatColor.RED + "You can't send shares to your self!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[2].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!Bukkit.getOfflinePlayer(args[1].toString()).hasPlayedBefore()) {
                    player.sendMessage(ChatColor.RED+"This player has never played before.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }

                sendToPlayer(args[1].toString(), args[2],Integer.parseInt(args[3]), player);
                break;

            case "fetch":
                if (!player.hasPermission("stocks.fetch")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks fetch <Stock>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[1].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                fetchStockDetails(player, args[1].toUpperCase());
                break;

            case "portfolio":
                if (!player.hasPermission("stocks.portfolio")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (args.length < 2) {
                    showPortfolio(player, player);
                } else {
                    if (!player.hasPermission("stocks.portfolio.other")) {
                        player.sendMessage(ChatColor.RED + "You do not have permission to view other's portfolios.");
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                        return false;
                    }
                    if (Bukkit.getOfflinePlayer(args[1].toString()).hasPlayedBefore()) {
                        showPortfolio(Bukkit.getOfflinePlayer(args[1].toString()), player);
                    } else {
                        player.sendMessage(ChatColor.RED + "That player has never played on this server.");
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    }
                }
                break;

            case "reload":
                if (!player.hasPermission("stocks.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return false;
                }
                plugin.reloadConfig();
                pricingHandler.Reload();
                stockHistory.CheckForNewCurrencies();
                player.sendMessage(ChatColor.GREEN + "All plugin configurations have been re-loaded.");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                break;

            case "help":
                if (!player.hasPermission("stocks.help")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return false;
                }
                if (player.hasPermission("stocks.help.admin")) {
                    player.sendMessage(ChatColor.AQUA + "You are viewing Admin's help menu.");
                    HelpMessage(player, true);
                    return false;
                } else {
                    HelpMessage(player, false);
                }
                break;
            case "crash":
                boolean crashEnabled = plugin.getConfig().getBoolean("Events.market-crash.enabled");
                boolean commandEnabled = plugin.getConfig().getBoolean("Events.market-crash.command-enabled");

                if (!player.hasPermission("stocks.crash")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return false;
                }
                if (!crashEnabled) {
                    player.sendMessage(ChatColor.RED + "This feature is not enabled in the config.");
                    return false;
                } else if (!commandEnabled) {
                    player.sendMessage(ChatColor.RED + "This command is not enabled in the config.");
                    return false;
                }

                if (args.length >= 3 && args[2].equalsIgnoreCase("confirm")) {
                    if (pendingCrash.containsKey(player)) {
                        String stockToCrash = pendingCrash.get(player);
                        pendingCrash.remove(player);

                        // Trigger the crash
                        plugin.crashMarket(stockToCrash);
                        player.sendMessage(ChatColor.GREEN + "Market crash confirmed for stock: " + ChatColor.YELLOW + stockToCrash);
                    } else {
                        player.sendMessage(ChatColor.RED + "No crash pending. Use /stocks crash <stock> to initiate.");
                    }
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks crash <stock>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }

                String stock = args[1];
                pendingCrash.put(player, stock);
                player.sendMessage(ChatColor.RED + "You are about to crash the market for stock: " + ChatColor.YELLOW + stock);
                player.sendMessage(ChatColor.RED + "Type " + ChatColor.YELLOW + "/stocks crash <stock> confirm " + ChatColor.RED + "within 10 seconds to confirm.");

                // Schedule a task to remove pending confirmation after 10 seconds
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (pendingCrash.containsKey(player)) {
                            pendingCrash.remove(player);
                            player.sendMessage(ChatColor.RED + "Market crash canceled due to timeout.");
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                        }
                    }
                }.runTaskLater(plugin, 200L); // 200 ticks = 10 seconds

                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Type " + ChatColor.YELLOW + "/stocks help" + ChatColor.RED + " for available commands.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                break;
        }
        return true;
    }

    private void sendToPlayer(String player, String stock, Integer amount, Player sender) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        Map<String, Integer> coins = playerData.fetchPlayerCoins(sender.getUniqueId().toString());
        if (coins.isEmpty()) {
            sender.sendMessage(ChatColor.RED+"You don't own any share yet.");
            sender.playSound(sender.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            return;
        }
        for (Map.Entry<String, Integer> entry : coins.entrySet()) {
            String stockName = entry.getKey();
            int CoinAmount = entry.getValue();

            if (Objects.equals(stockName, stock)) {
                if ( CoinAmount >= amount) {
                    CryptoFrenzy.getPlayerData().removeCoins(sender.getUniqueId().toString(), stock ,amount);
                    sender.sendMessage(ChatColor.GREEN + "Successfully sent "+amount+" share(s) of stock"+stock+" to "+player+" !");
                    sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    String playerUUID = Bukkit.getOfflinePlayer(player).getUniqueId().toString();

                    CryptoFrenzy.getPlayerData().addCoins(playerUUID, stock, amount, player);
                } else {
                    sender.sendMessage(ChatColor.RED+"You don't have enough shares of "+ChatColor.YELLOW+stockName+ChatColor.RED+", You currently have: "+ChatColor.YELLOW+CoinAmount);
                    sender.playSound(sender.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
            }
        }
    }

    private void removeStockFromPlayer(OfflinePlayer player, String stock, Integer amount, Player sender) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        if (!playerData.removeCoins(player.getUniqueId().toString(), stock, amount)) {
            sender.sendMessage(ChatColor.RED+"Error removing stock from player: Player does not own that amount of stocks.");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Successfully removed "+amount+" share(s) of "+stock+" from "+player.getName()+" !");
        pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, false);
        sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private void addStockToPlayer(OfflinePlayer player, String stock, Integer amount, Player sender) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        playerData.addCoins(player.getUniqueId().toString(), stock, amount, player.getName());
        sender.sendMessage(ChatColor.GREEN + "Successfully added "+amount+" share(s) of "+stock+" to "+player.getName()+" !");
        pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, true);
        sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private boolean handleCryptoFrenzyCommand(Player player) {
        String message = ChatColor.GOLD + "===== " + ChatColor.YELLOW + "CryptoFrenzy Plugin" + ChatColor.GOLD + " ======\n" +
                         ChatColor.WHITE + "Hello user! This plugin was created by 3lii3! You can find this plugin in the following links: \n" +
                         ChatColor.WHITE + "Discord: " + ChatColor.BLUE + ChatColor.ITALIC + ChatColor.UNDERLINE + "https://discord.gg/Fx8PrMH9Mr \n" +
                         ChatColor.WHITE + "SpigotMC: " + ChatColor.BLUE + ChatColor.ITALIC + ChatColor.UNDERLINE + "https://www.spigotmc.org/resources/cryptofrenzy.122061/ \n" +
                         ChatColor.WHITE + "GitHub: " + ChatColor.BLUE + ChatColor.ITALIC + ChatColor.UNDERLINE + "https://github.com/3lii3/CryptoFrenzy\n" +
                         ChatColor.GOLD + "==============================\n";
        player.sendMessage(message);
        return true;
    }

    private void listStocks(Player player) {
        FileConfiguration pricesConfig = plugin.getPricesConfig();
        String[] stocks = pricesConfig.getConfigurationSection("Stocks").getKeys(false).toArray(new String[0]);

        StringBuilder message = new StringBuilder();
        message.append(ChatColor.GOLD).append("===== ").append(ChatColor.YELLOW).append("STOCK MARKET").append(ChatColor.GOLD).append(" =====\n");
        pricingHandler.Reload();

        for (String stock : stocks) {
            int price = pricesConfig.getInt("Stocks." + stock + ".Price");
            int hourlyChange = pricingHandler.getPriceChange(stock, "1h");
            int dailyChange = pricingHandler.getPriceChange(stock, "24h");
            int weeklyChange = pricingHandler.getPriceChange(stock, "7d");

            message.append(ChatColor.YELLOW).append(stock)
                    .append(" ").append(ChatColor.GOLD).append(price).append("$")
                    .append(" ").append(getChangeColor(hourlyChange)).append(hourlyChange).append("%")
                    .append(" ").append(getChangeColor(dailyChange)).append(dailyChange).append("%")
                    .append(" ").append(getChangeColor(weeklyChange)).append(weeklyChange).append("%\n");
        }

        message.append(ChatColor.GOLD).append("========================");
        player.sendMessage(message.toString());
    }

    private void buyStock(Player player, String stock, int amount) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();
        Economy economy = CryptoFrenzy.getEconomy();

        // Calculation logic:
        int fee = plugin.getConfig().getInt("economy.market-fee");
        double tax = 1 + (plugin.getConfig().getDouble("economy.tax-rate") / 100);
        double basePrice = pricingHandler.calculatePriceForEachShare(stock.toUpperCase(), amount, true);
        double totalPrice = ((basePrice * tax )* amount) + fee;

        int maxAmount = plugin.getConfig().getInt("Stocks." + stock.toUpperCase() + ".max-shares");

        getLogger().info(player.getName() + " is trying to purchase " + amount + " shares of " + stock);
        Map<String, Integer> coins = playerData.fetchPlayerCoins(player.getUniqueId().toString());
        if (amount <= maxAmount ) {
            if (!coins.isEmpty()) {
                for (Map.Entry<String, Integer> entry : coins.entrySet()) {
                    String stockName = entry.getKey();
                    int CoinAmount = entry.getValue();
                    if (Objects.equals(stockName, stock)) {
                        if (CoinAmount + amount > maxAmount) {
                            getLogger().info(player.getName() + " tries to buy large amount of stocks");
                            player.sendMessage(ChatColor.RED + "You can't have more than " + ChatColor.YELLOW + maxAmount + ChatColor.RED + " shares of " + ChatColor.YELLOW + stock + ChatColor.RED + ", You currently have: " + ChatColor.YELLOW + CoinAmount);
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                        } else {
                            if (economy.has(player, totalPrice)) {
                                economy.withdrawPlayer(player, totalPrice);
                                getLogger().info(player.getName() + " bought " + amount + " shares of " + stock + " for " + totalPrice);
                                player.sendMessage(ChatColor.GREEN + "Successfully bought " + amount + " shares of " + stock + " for " + totalPrice + "$.");
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                                pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, true);

                                CryptoFrenzy.getPlayerData().addCoins(player.getUniqueId().toString(), stock, amount, player.getName());
                            } else {
                                getLogger().info(player.getName() + " lacking funds to buy stock.");
                                double AmountNeeded = totalPrice - economy.getBalance(player);
                                player.sendMessage(ChatColor.RED + "You need " + ChatColor.YELLOW + AmountNeeded + ChatColor.RED + " more to buy this amount of stocks!");
                                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                            }
                        }
                    }
                }
            } else {
                if (economy.has(player, totalPrice)) {
                    economy.withdrawPlayer(player, totalPrice);
                    getLogger().info(player.getName() + " bought " + amount + " shares of " + stock + " for " + totalPrice);
                    player.sendMessage(ChatColor.GREEN + "Successfully bought " + amount + " shares of " + stock + " for " + totalPrice + "$.");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, true);
                    CryptoFrenzy.getPlayerData().addCoins(player.getUniqueId().toString(), stock, amount, player.getName());
                } else {
                    getLogger().info(player.getName() + " lacking funds to buy stock.");
                    double AmountNeeded = totalPrice - economy.getBalance(player);
                    player.sendMessage(ChatColor.RED + "You need " + ChatColor.YELLOW + AmountNeeded + ChatColor.RED + " more to buy this amount of stocks!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
            }
        } else {
            getLogger().info(player.getName() + " tries to buy large amount of stocks");
            player.sendMessage(ChatColor.RED + "You can't have more than " + ChatColor.YELLOW + maxAmount + ChatColor.RED + " shares of " + ChatColor.YELLOW + stock + ChatColor.RED + " !");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        }
    }

    private void sellStock(Player player, String stock, int amount) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();
        Economy economy = CryptoFrenzy.getEconomy();

        Map<String, Integer> coins = playerData.fetchPlayerCoins(player.getUniqueId().toString());
        if (coins.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own any share of any stock!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            return;
        }

        for (Map.Entry<String, Integer> entry : coins.entrySet()) {
            String stockName = entry.getKey();
            int CoinAmount = entry.getValue();

            if (Objects.equals(stockName, stock)) {
                if (CoinAmount >= amount) {
                    CryptoFrenzy.getPlayerData().removeCoins(player.getUniqueId().toString(), stock, amount);

                    int fee = plugin.getConfig().getInt("economy.market-fee");
                    double taxRate = 1 - (plugin.getConfig().getDouble("economy.tax-rate") / 100);
                    double basePrice = pricingHandler.calculatePriceForEachShare(stock.toUpperCase(), amount, false);
                    double totalPrice = ((basePrice * taxRate)* amount) - fee;

                    economy.depositPlayer(player, totalPrice);

                    pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, false);

                    getLogger().info(player.getName() + " sold "+amount+" shares of "+stock+" for "+totalPrice);
                    player.sendMessage(ChatColor.GREEN + "Successfully sold " + amount + " shares of " + stock + " for " + totalPrice + "$ after tax.");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have enough shares of " + ChatColor.YELLOW + stockName + ChatColor.RED + ", You currently have: " + ChatColor.YELLOW + CoinAmount);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
            }
        }
    }

    private void fetchStockDetails(Player player, String stock) {
        FileConfiguration pricesConfig = plugin.getPricesConfig();

        String stockTD = stock.toUpperCase();

        int price = pricesConfig.getInt("Stocks." + stockTD + ".Price");
        int hourlyChange = pricingHandler.getPriceChange(stockTD, "1h");
        int dailyChange = pricingHandler.getPriceChange(stockTD, "24h");
        int weeklyChange = pricingHandler.getPriceChange(stockTD, "7d");

        FileConfiguration config = plugin.getConfig();

        long marketShares = config.getLong("Stocks."+stock.toUpperCase() +".market-shares");
        long marketCap = marketShares * price;
        long serverShares = config.getLong("Stocks."+stock.toUpperCase() +".server-shares");

        long playerShares = pricesConfig.getLong("Stocks."+stock.toUpperCase()+".playerMarket");

        String Description = config.getString("Stocks."+stock.toUpperCase() +".description");
        String StockName = config.getString("Stocks."+stock.toUpperCase() +".currency-name");

        String message = ChatColor.GOLD + "===== " + ChatColor.YELLOW + StockName + " (" + stock.toUpperCase() + ")" +
                        ChatColor.GOLD + " =====\n" +
                        ChatColor.YELLOW + "Description: " + ChatColor.WHITE + Description +
                        ChatColor.YELLOW + "\n\nMarket Cap: " + ChatColor.WHITE + marketCap +
                        ChatColor.YELLOW + "\nCirculating Shares: " + ChatColor.WHITE + marketShares +
                        ChatColor.YELLOW + "\n\n Server Shares:" + ChatColor.WHITE + serverShares +
                        ChatColor.YELLOW + "\n Available Shares:" + ChatColor.WHITE + playerShares +
                        ChatColor.WHITE + "\n\nPrice: " + ChatColor.YELLOW + price + "$ " +
                        ChatColor.WHITE + "1h: " + getChangeColor(hourlyChange) + hourlyChange + "% " +
                        ChatColor.WHITE + "24h: " + getChangeColor(dailyChange) + dailyChange + "% " +
                        ChatColor.WHITE + "7d: " + getChangeColor(weeklyChange) + weeklyChange + "% " +
                        ChatColor.GOLD + "\n========================";

        player.sendMessage(message);
    }

    private ChatColor getChangeColor(int change) {
        if (change > 0) {
            return ChatColor.GREEN;
        } else if (change < 0) {
            return ChatColor.RED;
        } else {
            return ChatColor.YELLOW;
        }
    }

    private void HelpMessage(Player player, boolean isAdmin) {
        StringBuilder message = new StringBuilder();
        if (!isAdmin) {
            message.append(ChatColor.GOLD).append("===== ").append(ChatColor.YELLOW).append("Help menu").append(ChatColor.GOLD).append(" =====\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks help ").append(ChatColor.WHITE).append("Shows this menu.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks buy <Stock> <Amount> ").append(ChatColor.WHITE).append("Buys specific amount of stocks.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks sell <Stock> <Amount> ").append(ChatColor.WHITE).append("Sells specific amount of stocks.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks fetch <Stock> ").append(ChatColor.WHITE).append("Fetches information about specific stock.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks list ").append(ChatColor.WHITE).append("Lists available stocks.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks portfolio (player) ").append(ChatColor.WHITE).append("Shows player's portfolio\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks send <Player> <Stock> <Amount> ").append(ChatColor.WHITE).append("Sends stocks to another player/\n");
            message.append(ChatColor.GOLD).append("========================");

            player.sendMessage(message.toString());
        } else {
            message.append(ChatColor.GOLD).append("===== ").append(ChatColor.YELLOW).append("Admin Help menu").append(ChatColor.GOLD).append(" =====\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks help ").append(ChatColor.WHITE).append("Shows this menu.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks buy <Stock> <Amount> ").append(ChatColor.WHITE).append("Buys specific amount of stocks.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks sell <Stock> <Amount> ").append(ChatColor.WHITE).append("Sells specific amount of stocks.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks fetch <Stock> ").append(ChatColor.WHITE).append("Fetches information about specific stock.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks list ").append(ChatColor.WHITE).append("Lists available stocks.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks portfolio (player) ").append(ChatColor.WHITE).append("Shows player's portfolio\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks send <Player> <Stock> <Amount> ").append(ChatColor.WHITE).append("Sends stocks to another player/\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks add <Player> <Stock> <Amount> ").append(ChatColor.WHITE).append("Adds shares to a player\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks remove <Player> <Stock> <Amount> ").append(ChatColor.WHITE).append("Removes shares from a player\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks crash <Stock> ").append(ChatColor.RED).append("*Dangerous command* ").append(ChatColor.WHITE).append("Crashes specified market.\n");
            message.append(ChatColor.WHITE).append("- ").append(ChatColor.YELLOW).append("/stocks reload ").append(ChatColor.RED).append("*Dangerous command* ").append(ChatColor.WHITE).append(" Reloads all configurations\n");
            message.append(ChatColor.GOLD).append("========================");

            player.sendMessage(message.toString());
        }
    }

    private void showPortfolio(OfflinePlayer player, Player sender) {
        plugin.reloadPricesConfig();
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        Map<String, Integer> coins = playerData.fetchPlayerCoins(player.getUniqueId().toString());

        StringBuilder message = new StringBuilder();
        message.append(ChatColor.GOLD).append("===== ").append(ChatColor.YELLOW).append(player.getName()).append("'s Portfolio").append(ChatColor.GOLD).append(" =====\n");

        // Check if the player has any coins in their portfolio
        int totalCoins = coins.values().stream().mapToInt(Integer::intValue).sum();
        if (totalCoins == 0) {
            message.append(ChatColor.WHITE).append(ChatColor.ITALIC).append("No ongoing investments yet.\n");
        } else {
            message.append(ChatColor.WHITE).append(ChatColor.ITALIC).append("Ongoing investments:\n");

            for (Map.Entry<String, Integer> entry : coins.entrySet()) {
                String stockName = entry.getKey();
                int amount = entry.getValue();

                pricingHandler.Reload();

                if (amount > 0) {

                    int price = pricesConfig.getInt("Stocks." + stockName + ".Price");

                    message.append(ChatColor.GOLD).append(stockName)
                            .append(ChatColor.YELLOW).append(": ").append(ChatColor.GOLD).append(amount)
                            .append(ChatColor.YELLOW).append(" share(s) at: ")
                            .append(ChatColor.GOLD).append(price).append("$")
                            .append(ChatColor.YELLOW).append(" each.\n");
                }
            }
        }
        message.append(ChatColor.GOLD).append("========================");

        sender.sendMessage(message.toString());
    }

    private Logger getLogger() {
        return Bukkit.getLogger();
    }
}
