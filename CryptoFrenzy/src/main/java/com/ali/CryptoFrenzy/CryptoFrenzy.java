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
    private static PricingHandler pricingHandler;
    private static MarketCrashEvent marketCrashEvent;

    private Connection playerDBConnection;
    private Connection stockDBConnection;

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
        saveDefaultConfig();
        if (!generatePricesFile()) {
            getLogger().warning("Failed to generate Prices.yml.");
        } else {
            loadPricesConfig();
        }

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
            if (economy == null) {
                getLogger().severe("No economy provider found! Please install an economy plugin for the plugin to function.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("Vault integration enabled.");
        } else {
            getLogger().warning("Vault not found! Please add Vault for plugin to work properly!");
        }

        connectToStockDB();
        connectToPlayerDB();

        marketCrashEvent = new MarketCrashEvent(this);
        pricingHandler = new PricingHandler(this);
        playerData = new PlayerData(playerDBConnection, this);
        stockHistory = new StockHistory(stockDBConnection, this);

        schedulePriceUpdate();

        if (stockDBConnection == null && playerDBConnection == null) {
            getLogger().severe("stockhistory.db NOT FOUND");
            getLogger().severe("playerdata.db NOT FOUND");
            getLogger().severe("PLEASE REPORT ERRORS AND SEND LOGS TO DEVS");
            getServer().getPluginManager().disablePlugin(this);
        } else if (stockDBConnection == null) {
            getLogger().severe("stockhistory.db NOT FOUND");
            getLogger().severe("PLEASE REPORT ERRORS AND SEND LOGS TO DEVS");
            getServer().getPluginManager().disablePlugin(this);
        } else if (playerDBConnection == null) {
            getLogger().severe("playerdata.db NOT FOUND");
            getLogger().severe("PLEASE REPORT ERRORS AND SEND LOGS TO DEVS");
            getServer().getPluginManager().disablePlugin(this);
        }
        this.getCommand("stocks").setExecutor(new StocksCommand(this,stockHistory));
        getCommand("stocks").setTabCompleter(new StocksTabCompleter(this));

        this.getCommand("cryptofrenzy").setExecutor(new StocksCommand(this,stockHistory));
        getCommand("cryptofrenzy").setTabCompleter(new StocksTabCompleter(this));
    }

    public void schedulePriceUpdate() {
        long delayInMinutes = getConfig().getLong("DatabaseUpdateFrequency");
        long delayInSeconds = delayInMinutes * 60;

        new BukkitRunnable() {
            @Override
            public void run() {
                stockHistory.updateAllStockHistory();
                stockHistory.removeOldHistory();
                pricingHandler.updatePricesYMLFromHistory();
            }
        }.runTaskTimer(this, 0L, delayInSeconds * 20L);
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

    private void connectToPlayerDB() {
        getLogger().info("Trying to initiate a connection to the database.");

        try {
            String url = "jdbc:sqlite:" + getDataFolder() + File.separator + "playerdata.db";

            // SQLite does not require the database to be created separately
            playerDBConnection = DriverManager.getConnection(url);

            playerData = new PlayerData(playerDBConnection ,this);
            getLogger().info("Database connection established.");

            playerData.createTableIfNotExists();

        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().warning("Failed to connect to the database.");
        }
    }

    public Connection getStockDBConnection() {
        return this.stockDBConnection;
    }

    private void connectToStockDB() {
        getLogger().info("Trying to initiate a connection to the database.");

        try {
            String url = "jdbc:sqlite:" + getDataFolder() + File.separator + "stockhistory.db";

            // SQLite does not require the database to be created separately
            stockDBConnection = DriverManager.getConnection(url);

            stockHistory = new StockHistory(stockDBConnection, this);
            getLogger().info("Database connection established.");

            stockHistory.createTableIfNotExists();

        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().warning("Failed to connect to the database.");
        }
    }

    private void disconnectFromDatabase() {
        if (playerDBConnection != null) {
            try {
                playerDBConnection.close();
                getLogger().info("Player database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
                getLogger().warning("Failed to close the player database connection.");
            }
        }

        if (stockDBConnection != null) {
            try {
                stockDBConnection.close();
                getLogger().info("Stock database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
                getLogger().warning("Failed to close the stock database connection.");
            }
        }
    }

    public static PlayerData getPlayerData() {
        return playerData;
    }

    public static StockHistory getStockHistory() {
        return stockHistory;
    }

    public void crashMarket(String stock) {
        marketCrashEvent.triggerCrash(stock);
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
                pricesConfig.set(stockPath + ".1h", stockData.get("price"));
                pricesConfig.set(stockPath + ".24h", stockData.get("price"));
                pricesConfig.set(stockPath + ".7d", stockData.get("price"));
                pricesConfig.set(stockPath + ".totalShares", stockData.get("market-shares"));
                pricesConfig.set(stockPath + ".market-shares", stockData.get("market-shares"));
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
