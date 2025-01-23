package com.ali.CryptoFrenzy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

import java.sql.Connection;

public class PricingHandler {

    private FileConfiguration pricesConfig;
    private File pricesFile;
    final private CryptoFrenzy plugin;
    private StockHistory stockHistory;

    public PricingHandler(CryptoFrenzy plugin) {
        this.plugin = plugin;
        loadPricesFile();

        Connection stockDBConnection = plugin.getStockDBConnection();

        this.stockHistory = new StockHistory(stockDBConnection, plugin);
    }

    public void Reload() {
        pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) {
            plugin.saveResource("prices.yml", false);
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
    }

    // Load or create the prices.yml file
    private void loadPricesFile() {
        pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) {
            plugin.saveResource("prices.yml", false); // Saves the default prices.yml from the jar if it doesn't exist
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);
    }

    // Method to get stock price
    public double getPrice(String stockName) {plugin.reloadPricesConfig(); return pricesConfig.getDouble("Stocks." + stockName + ".Price"); }

    // Method to get the percentage change for 1h, 24h, and 7d
    public int getPriceChange(String stockName, String timeFrame) {
        plugin.reloadPricesConfig();
        double currentPrice = getPrice(stockName);
        double oldPrice = pricesConfig.getDouble("Stocks." + stockName + "." + timeFrame);
        return calculatePercentageChange(currentPrice, oldPrice);
    }

    // Helper method to calculate percentage change
    public int calculatePercentageChange(double currentPrice, double oldPrice) {
        if (oldPrice == 0) {
            return 0; // Avoid division by zero
        }
        return (int) (((currentPrice - oldPrice) / oldPrice) * 100);
    }

    public boolean adjustPriceBasedOnSupplyDemand(String stockName, int amount, boolean isBuying) {
        FileConfiguration config = plugin.getConfig();
        plugin.reloadPricesConfig();
        if (!pricesConfig.contains("Stocks." + stockName)) {
            plugin.getLogger().warning("Stock not found in Prices.yml: " + stockName);
            return false;
        }

        double basePrice = config.getDouble("Stocks." + stockName + ".price");
        long supply = pricesConfig.getLong("Stocks." + stockName + ".totalShares");
        long demand = pricesConfig.getLong("Stocks." + stockName + ".market-shares");

        if (isBuying) {
            demand += amount;
        } else {
            demand -= amount;
        }

        double priceAdjustmentFactor = 1 + ((double)(demand - supply) / supply);
        double newPrice = basePrice * priceAdjustmentFactor;

        pricesConfig.set("Stocks." + stockName + ".Price", newPrice);
        pricesConfig.set("Stocks." + stockName + ".market-shares", demand);
        pricesConfig.set("Stocks." + stockName + ".totalShares", supply);

        try {
            pricesConfig.save(pricesFile);
            plugin.getLogger().info("Stock price for " + stockName + " adjusted to: " + newPrice + " (Demand: " + demand + ", Supply: "+ supply +")");
            plugin.reloadPricesConfig();
            Reload();
            plugin.getLogger().info("Reloaded prices config to update prices.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double calculatePriceForEachShare(String stockName, int amount, boolean isBuying) {
        plugin.reloadPricesConfig();
        double basePrice = plugin.getConfig().getDouble("Stocks." + stockName + ".price");
        double supply = pricesConfig.getLong("Stocks." + stockName + ".totalShares");
        double demand = pricesConfig.getLong("Stocks." + stockName + ".market-shares");

        if (isBuying) {
            demand += amount;
        } else {
            demand -= amount;
        }

        double formula = 1 + (demand - supply) / supply;
        double newPrice = basePrice * formula;

        return newPrice;
    }

    public void updatePricesYMLFromHistory() {
        plugin.reloadPricesConfig();
        for (String stock : pricesConfig.getConfigurationSection("Stocks").getKeys(false)) {

            Double hourPrice = stockHistory.getStock1hPrice(stock);
            Double dayPrice = stockHistory.getStock24hPrice(stock);
            Double weekPrice = stockHistory.getStock7dPrice(stock);

            pricesConfig.set("Stocks." + stock + ".1h", hourPrice);
            pricesConfig.set("Stocks." + stock + ".24h", dayPrice);
            pricesConfig.set("Stocks." + stock + ".7d", weekPrice);
        }
    }
}
