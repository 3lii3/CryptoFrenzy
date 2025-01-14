package com.ali.CryptoFrenzy;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.OfflinePlayer;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Sound;

import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.UUID;

public class StocksCommand implements CommandExecutor {

    private CryptoFrenzy plugin;
    PricingHandler pricingHandler;
    private FileConfiguration pricesConfig;
    private StockHistory stockHistory;

    // To store the last command execution times for each player
    private Map<Player, Long> lastCommandTime = new HashMap<>();

    public StocksCommand(CryptoFrenzy plugin) {
        this.plugin = plugin;
        this.pricingHandler = new PricingHandler(plugin);
        this.pricesConfig = plugin.getPricesConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        // Check if cooldown is enabled in config
        boolean cooldownEnabled = plugin.getConfig().getBoolean("player.cooldown.enabled");
        boolean hasCooldownTag = player.hasPermission("stocks.cooldown");

        // Apply cooldown only for buy/sell commands and if the player doesn't have stocks.cooldown tag
        if (cooldownEnabled && !hasCooldownTag && (command.getName().equalsIgnoreCase("stock") && (args.length > 0 && (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("sell"))))) {
            int cooldownTime = plugin.getConfig().getInt("player.cooldown.time") * 1000; // Convert to milliseconds
            long currentTime = System.currentTimeMillis();
            long lastTime = lastCommandTime.getOrDefault(player, 0L);

            if (currentTime - lastTime < cooldownTime) {
                long timeRemaining = (cooldownTime - (currentTime - lastTime)) / 1000;
                player.sendMessage(ChatColor.RED + "You must wait " + timeRemaining + " seconds before using this command again.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                return false;
            }

            // Update the last command time
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
        // This is where your current stocks command logic goes.
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
                if (!pricesConfig.contains("Stocks." + args[1].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks buy <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }

                try {
                    int amount = Integer.parseInt(args[2]);
                    buyStock(player, args[1], amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount! Please enter a valid number.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }

            case "sell":
                if (!player.hasPermission("stocks.sell")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[1].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks sell <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                sellStock(player, args[1].toUpperCase(), Integer.parseInt(args[2]));
                break;

            case "remove":
                if (!player.hasPermission("stocks.remove")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[2].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks remove <Player> <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                removeStockFromPlayer(args[1], args[2],Integer.parseInt(args[3]), player);
                break;

            case "add":
                if (!player.hasPermission("stocks.add")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[2].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks add <Player> <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                addStockToPlayer(args[1], args[2],Integer.parseInt(args[3]), player);
                break;

            case "send":
                if (!player.hasPermission("stocks.send")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[2].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks send <Player> <Stock> <Amount>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                sendToPlayer(args[1], args[2],Integer.parseInt(args[3]), player);
                break;

            case "fetch":
                if (!player.hasPermission("stocks.fetch")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
                }
                if (!pricesConfig.contains("Stocks." + args[1].toUpperCase())) {
                    player.sendMessage(ChatColor.RED + "The stock " + args[1].toUpperCase() + " does not exist.");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    break;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Wrong command usage! Usage: " + ChatColor.YELLOW + "/stocks fetch <Stock>");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    return false;
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
                    OfflinePlayer searchPlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (searchPlayer.hasPlayedBefore()) {
                        showPortfolio(searchPlayer, player);
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
                plugin.reloadPricesConfig();
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
                HelpMessage(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Type " + ChatColor.YELLOW + "/stocks help" + ChatColor.RED + " for available commands.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                break;
        }
        return true;
    }

    private boolean sendToPlayer(String player, String stock, Integer amount, Player sender) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        Map<String, Integer> coins = playerData.fetchPlayerCoins(sender.getUniqueId().toString());
        for (Map.Entry<String, Integer> entry : coins.entrySet()) {
            String stockName = entry.getKey();
            int CoinAmount = entry.getValue();
            Economy economy = CryptoFrenzy.getEconomy();

            if (Objects.equals(stockName, stock)) {
                if ( CoinAmount >= amount) {
                    CryptoFrenzy.getPlayerData().removeCoins(sender.getUniqueId().toString(), stock ,amount);

                    double totalAmountReceived = pricingHandler.calculatePriceForEachShare(stock, amount, false);
                    economy.depositPlayer(player, totalAmountReceived);

                    pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, false);

                    sender.sendMessage(ChatColor.GREEN + "Successfully sold " + amount + " " + stock + " stock for " + totalAmountReceived + "$!");
                    sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    sender.sendMessage(ChatColor.RED+"You don't have enough shares of "+ChatColor.YELLOW+stockName+ChatColor.RED+", You currently have: "+ChatColor.YELLOW+CoinAmount);
                    sender.playSound(sender.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                }
            }
        }

        return true;
    }

    private boolean removeStockFromPlayer(String player, String stock, Integer amount, Player sender) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        if (!playerData.playerExists(String.valueOf((UUID.fromString(String.valueOf(player)))))) {
            sender.sendMessage(ChatColor.RED+"This player does not exist.");
            sender.playSound(sender.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            return false;
        }

        UUID playerUUID = UUID.fromString(player);
        String playerString = String.valueOf(playerUUID);

        playerData.removeCoins(playerString, stock, amount);

        sender.sendMessage(ChatColor.GREEN + "Successfully removed "+amount+" share(s) of "+stock+" from "+player+" !");
        sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        return true;
    }

    private boolean addStockToPlayer(String player, String stock, Integer amount, Player sender) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        if (!playerData.playerExists(String.valueOf((UUID.fromString(String.valueOf(player)))))) {
            sender.sendMessage(ChatColor.RED+"This player does not exist.");
            sender.playSound(sender.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            return false;
        }

        UUID playerUUID = UUID.fromString(player);
        String playerString = String.valueOf(playerUUID);

        playerData.removeCoins(playerString, stock, amount);
        sender.sendMessage(ChatColor.GREEN + "Successfully added "+amount+" share(s) of "+stock+" to "+player+" !");
        sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        return true;
    }

    private boolean handleCryptoFrenzyCommand(Player player) {
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.DARK_GRAY).append("===== ").append(ChatColor.YELLOW).append("CryptoFrenzy Plugin").append(ChatColor.DARK_GRAY).append(" =====\n");
        message.append(ChatColor.YELLOW).append("Hello user! This plugin was created by 3lii3! You can find this plugin in the following link: \n");
        TextComponent url = new TextComponent("https://www.spigotmc.org/resources/*-coming soon-* \n");
        url.setColor(ChatColor.BLUE);
        url.setItalic(true);
        url.setUnderlined(true);
        player.spigot().sendMessage(new TextComponent(message.toString()));
        player.spigot().sendMessage(url);
        message.append(ChatColor.DARK_GRAY).append("==============================\n");
        player.sendMessage(message.toString());
        return true;
    }

    private void listStocks(Player player) {
        FileConfiguration pricesConfig = plugin.getPricesConfig();
        String[] stocks = pricesConfig.getConfigurationSection("Stocks").getKeys(false).toArray(new String[0]);

        StringBuilder message = new StringBuilder();
        message.append(ChatColor.DARK_GRAY).append("===== ").append(ChatColor.YELLOW).append("STOCK MARKET").append(ChatColor.DARK_GRAY).append(" =====\n");

        for (String stock : stocks) {
            int price = pricesConfig.getInt("Stocks." + stock + ".Price");
            int hourlyChange = pricingHandler.getPriceChange(stock, "1h");
            int dailyChange = pricingHandler.getPriceChange(stock, "24h");
            int weeklyChange = pricingHandler.getPriceChange(stock, "7d");

            message.append(ChatColor.GRAY).append(stock)
                    .append(" ").append(ChatColor.BLUE).append(price).append("$")
                    .append(" ").append(getChangeColor(hourlyChange)).append(hourlyChange).append("%")
                    .append(" ").append(getChangeColor(dailyChange)).append(dailyChange).append("%")
                    .append(" ").append(getChangeColor(weeklyChange)).append(weeklyChange).append("%\n");
        }

        message.append(ChatColor.DARK_GRAY).append("========================");
        player.sendMessage(message.toString());
    }

    private void buyStock(Player player, String stock, int amount) {
        Economy economy = CryptoFrenzy.getEconomy();
        int fee = plugin.getConfig().getInt("economy.market-fee");
        int tax = plugin.getConfig().getInt("economy.tax-rate");

        double Price =  pricingHandler.calculatePriceForEachShare(stock, amount, true) + fee;

        if (economy.has(player, Price)) {
            economy.withdrawPlayer(player, Price);
            player.sendMessage(ChatColor.GREEN+"Successfully bought " + amount + " shares of " + stock + " for " + Price + "$.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, true);
            CryptoFrenzy.getPlayerData().addCoins(player.getUniqueId().toString(), stock ,amount, player.getName());
        } else {

            double AmountNeeded = Price - economy.getBalance(player);
            player.sendMessage(ChatColor.RED+"You need "+ChatColor.YELLOW+AmountNeeded+ChatColor.RED+" more to buy this amount of stocks!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
        }
    }

    private void sellStock(Player player, String stock, int amount) {
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        Map<String, Integer> coins = playerData.fetchPlayerCoins(player.getUniqueId().toString());
        for (Map.Entry<String, Integer> entry : coins.entrySet()) {
            String stockName = entry.getKey();
            int CoinAmount = entry.getValue();
            Economy economy = CryptoFrenzy.getEconomy();

            if (Objects.equals(stockName, stock)) {
                if ( CoinAmount >= amount) {
                    CryptoFrenzy.getPlayerData().removeCoins(player.getUniqueId().toString(), stock ,amount);

                    double totalAmountReceived = pricingHandler.calculatePriceForEachShare(stock, amount, false);
                    economy.depositPlayer(player, totalAmountReceived);

                    pricingHandler.adjustPriceBasedOnSupplyDemand(stock, amount, false);

                    player.sendMessage(ChatColor.GREEN + "Successfully sold " + amount + " " + stock + " stock for " + totalAmountReceived + "$!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } else {
                    player.sendMessage(ChatColor.RED+"You don't have enough shares of "+ChatColor.YELLOW+stockName+ChatColor.RED+", You currently have: "+ChatColor.YELLOW+CoinAmount);
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

        int marketShares = (int) config.getLong("Stocks."+stock.toUpperCase() +".market-shares");
        int marketCap = marketShares * price;

        String Description = config.getString("Stocks."+stock.toUpperCase() +".description");
        String StockName = config.getString("Stocks."+stock.toUpperCase() +".currency-name");

        StringBuilder message = new StringBuilder();
        message.append(ChatColor.DARK_GRAY).append("===== ").append(ChatColor.AQUA).append(StockName).append(" (").append(stock.toUpperCase())
                .append(ChatColor.DARK_GRAY).append(") =====\n")
                .append(ChatColor.YELLOW).append("Description: ").append(ChatColor.WHITE).append(Description)
                .append(ChatColor.YELLOW).append("\nStock Market-Cap:").append(ChatColor.WHITE).append(marketCap)
                .append(ChatColor.YELLOW).append("\nStock Market Shares:").append(ChatColor.WHITE).append(marketShares)
                .append(ChatColor.GRAY).append("\nPrice: ").append(ChatColor.BLUE).append(price).append("$ ")
                .append(ChatColor.GRAY).append("1h: ").append(getChangeColor(hourlyChange)).append(hourlyChange).append("% ")
                .append(ChatColor.GRAY).append("24h: ").append(getChangeColor(dailyChange)).append(dailyChange).append("% ")
                .append(ChatColor.GRAY).append("7d: ").append(getChangeColor(weeklyChange)).append(weeklyChange).append("% ");
        message.append(ChatColor.DARK_GRAY).append("\n========================");

        player.sendMessage(message.toString());
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

    private void HelpMessage(Player player) {
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.DARK_GRAY).append("===== ").append(ChatColor.AQUA).append("Help menu").append(ChatColor.DARK_GRAY).append(" =====\n");
        message.append(ChatColor.DARK_GRAY).append("- ").append(ChatColor.YELLOW).append("/stocks help ").append(ChatColor.GREEN).append("Shows this menu.\n");
        message.append(ChatColor.DARK_GRAY).append("- ").append(ChatColor.YELLOW).append("/stocks buy <Stock> <Amount> ").append(ChatColor.GREEN).append("Buys specific amount of stocks.\n");
        message.append(ChatColor.DARK_GRAY).append("- ").append(ChatColor.YELLOW).append("/stocks sell <Stock> <Amount> ").append(ChatColor.GREEN).append("Sells specific amount of stocks.\n");
        message.append(ChatColor.DARK_GRAY).append("- ").append(ChatColor.YELLOW).append("/stocks fetch <Stock> ").append(ChatColor.GREEN).append("Fetches information about specific stock.\n");
        message.append(ChatColor.DARK_GRAY).append("- ").append(ChatColor.YELLOW).append("/stocks list <Stock> ").append(ChatColor.GREEN).append("Lists available stocks & their information.\n");
        message.append(ChatColor.DARK_GRAY).append("- ").append(ChatColor.YELLOW).append("/stocks portfolio ").append(ChatColor.GREEN).append("Shows your shares in stocks.\n");
        message.append(ChatColor.DARK_GRAY).append("========================");

        player.sendMessage(message.toString());
    }

    private boolean showPortfolio(OfflinePlayer player, Player sender) {
        // Get PlayerData instance
        PlayerData playerData = CryptoFrenzy.getPlayerData();

        if (!playerData.playerExists(String.valueOf((UUID.fromString(String.valueOf(player)))))) {
            sender.sendMessage(ChatColor.RED+"This player does not exist.");
            return false;
        }

        // Fetch the player's coin data using their UUID
        Map<String, Integer> coins = playerData.fetchPlayerCoins(player.getUniqueId().toString());

        // Build the message to display to the player
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.DARK_GRAY).append("===== ").append(ChatColor.AQUA).append(player.getName()).append("'s Portfolio").append(ChatColor.DARK_GRAY).append(" =====\n");

        // Check if the player has any coins in their portfolio
        int totalCoins = coins.values().stream().mapToInt(Integer::intValue).sum(); // Sum up all the coin amounts
        if (totalCoins == 0) {
            message.append(ChatColor.DARK_GRAY).append("No ongoing investments yet.\n");
        } else {
            message.append(ChatColor.GRAY).append("Ongoing investments:\n");

            // Loop through each stock and show the amount the player owns
            for (Map.Entry<String, Integer> entry : coins.entrySet()) {
                String stockName = entry.getKey();
                int amount = entry.getValue();

                if (amount > 0) {

                    int hourlyChange = pricingHandler.getPriceChange(stockName, "1h");
                    int dailyChange = pricingHandler.getPriceChange(stockName, "24h");
                    int weeklyChange = pricingHandler.getPriceChange(stockName, "7d");

                    int price = pricesConfig.getInt("Stocks." + stockName + ".Price");

                    message.append(ChatColor.YELLOW).append(stockName)
                            .append(": ").append(ChatColor.GREEN).append(amount)
                            .append(" share(s)").append(ChatColor.GRAY)
                            .append(" at ").append(ChatColor.YELLOW).append(price).append("$").append(ChatColor.GRAY)
                            .append(" each.");

                    message.append("\n")
                            .append(ChatColor.GRAY).append("1h: ").append(getChangeColor(hourlyChange)).append(hourlyChange).append("% ")
                            .append(ChatColor.GRAY).append("24h: ").append(getChangeColor(dailyChange)).append(dailyChange).append("% ")
                            .append(ChatColor.GRAY).append("7d: ").append(getChangeColor(weeklyChange)).append(weeklyChange).append("% ")
                            .append("\n");
                }
            }
        }

        message.append(ChatColor.DARK_GRAY).append("========================");

        // Send the message to the player
        sender.sendMessage(message.toString());
        return true;
    }
}
