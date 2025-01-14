package com.ali.CryptoFrenzy;

import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.util.Map;

public class CryptoFrenzy extends JavaPlugin {

    private static Economy economy;
    private static StockHistory stockHistory;
    private static PlayerData playerData;
    private static Connection connection;

    private File pricesFile;
    private FileConfiguration pricesConfig;

    public static List<String> AvailableStocks() {
        List<String> stockNames = new ArrayList<>();

        // Access the plugin's configuration file
        FileConfiguration config = CryptoFrenzy.getPlugin(CryptoFrenzy.class).getConfig();

        // Retrieve the stock names from the config section
        Set<String> stockKeys = config.getConfigurationSection("Stocks").getKeys(false);

        // Add each stock name to the list
        for (String stockName : stockKeys) {
            stockNames.add(stockName);
        }

        return stockNames;
    }

    @Override
    public void onEnable() {
        boolean generated = generatePricesFile();
        while (generated) break;
        saveDefaultConfig();
        loadPricesConfig();

        this.getCommand("stocks").setExecutor(new StocksCommand(this));
        getCommand("stocks").setTabCompleter(new StocksTabCompleter(this));

        this.getCommand("cryptofrenzy").setExecutor(new StocksCommand(this));
        getCommand("cryptofrenzy").setTabCompleter(new StocksTabCompleter(this));

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
            getLogger().info("Vault integration enabled.");
        } else {
            getLogger().warning("Vault not found! Please add Vault for plugin to work properly!");
        }

        connectToPlayerDB(this);
        connectToStockDB(this);
        saveDefaultConfig();
        schedulePriceUpdate();
    }

    public void schedulePriceUpdate() {
        // Run this task every 30 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                stockHistory.updateAllStockHistory();
                stockHistory.removeOldHistory();
            }
        }.runTaskTimer(this, 0L, 300L * 20L);
    }


    @Override
    public void onDisable() {
        disconnectFromDatabase();
    }

    public static Economy getEconomy() {
        return economy;
    }

    private void checkNewStocks() {
        getStockHistory().CheckForNewCurrencies();
    }

    private void connectToPlayerDB(CryptoFrenzy plugin) {
        try {
            String host = plugin.getConfig().getString("database.host");
            int port = plugin.getConfig().getInt("database.port");
            String username = plugin.getConfig().getString("database.user");
            String password = plugin.getConfig().getString("database.password");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + "playerdata";
            connection = DriverManager.getConnection(url, username, password);

            playerData = new PlayerData(connection, this);
            getLogger().info("Database connection established.");

            // Create table if it doesn't exist
            playerData.createTableIfNotExists();
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().warning("Failed to connect to the database.");
        }
    }

    private void connectToStockDB(CryptoFrenzy plugin) {
        try {
            String host = plugin.getConfig().getString("database.host");
            int port = plugin.getConfig().getInt("database.port");
            String username = plugin.getConfig().getString("database.user");
            String password = plugin.getConfig().getString("database.password");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + "stockhistory";
            connection = DriverManager.getConnection(url, username, password);

            stockHistory = new StockHistory(connection, this);
            getLogger().info("Database connection established.");

            // Create table if it doesn't exist
            stockHistory.createTableIfNotExists();
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().warning("Failed to connect to the database.");
        }
    }

    private void disconnectFromDatabase() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
                getLogger().warning("Failed to close the database connection.");
            }
        }
    }

    public static PlayerData getPlayerData() {
        return playerData;
    }

    public static StockHistory getStockHistory() {
        return stockHistory;
    }

    public void loadPricesConfig() {
        pricesFile = new File(getDataFolder(), "Prices.yml");
        if (!pricesFile.exists()) {
            pricesFile.getParentFile().mkdirs();
            saveResource("Prices.yml", false);
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
    }

    public FileConfiguration getPricesConfig() {
        if (pricesConfig == null) {
            loadPricesConfig();
        }
        return pricesConfig;
    }

    public void reloadPricesConfig() {
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
        checkNewStocks();
    }

    public boolean generatePricesFile() {
        // Load the main config file
        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Create a reference to the Prices.yml file
        File pricesFile = new File(getDataFolder(), "Prices.yml");

        // Check if the Prices.yml file already exists
        if (pricesFile.exists()) {
            getLogger().info("Prices.yml already exists. Skipping generation.");
            return true;  // If it exists, skip generating the file
        }

        // If Prices.yml does not exist, create a new Prices.yml file
        YamlConfiguration pricesConfig = new YamlConfiguration();

        // Check if "Stocks" exists in the config file
        if (config.contains("Stocks")) {
            // Loop through the Stocks in the config
            for (String stockName : config.getConfigurationSection("Stocks").getKeys(false)) {
                // Get the stock data map
                Map<String, Object> stockData = config.getConfigurationSection("Stocks." + stockName).getValues(false);

                // Prepare the new stock entry for Prices.yml
                String stockPath = "Stocks." + stockName;

                // Set price and historical data under the stock name
                pricesConfig.set(stockPath + ".Price", stockData.get("price"));
                pricesConfig.set(stockPath + ".1h", 0);
                pricesConfig.set(stockPath + ".24h", 0);
                pricesConfig.set(stockPath + ".7d", 0);
                pricesConfig.set(stockPath + ".totalShares", stockData.get("market-shares"));
                pricesConfig.set(stockPath + ".market-shares", 0);
            }

            // Save the Prices.yml file
            try {
                pricesConfig.save(pricesFile);
                getLogger().info("Prices.yml has been successfully generated!");
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            getLogger().warning("No stocks found in the config.");
        }
        return true;
    }
}
