package com.ali.CryptoFrenzy;

import java.util.logging.Logger;

import java.sql.*;
import java.util.logging.Level;
import java.util.List;

import static org.bukkit.Bukkit.getLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class StockHistory {

    private Connection connection;
    private CryptoFrenzy plugin; // Add the plugin field
    private static final Logger logger = Logger.getLogger(StockHistory.class.getName());

    // Modify the constructor to accept the plugin as a parameter
    public StockHistory(Connection connection, CryptoFrenzy plugin) {
        this.connection = connection;
        this.plugin = plugin;
    }

    public void updateAllStockHistory() {
        // Load the Prices.yml file
        File pricesFile = new File(plugin.getDataFolder(), "Prices.yml");
        if (!pricesFile.exists()) {
            getLogger().warning("Prices.yml does not exist. Unable to update stock history.");
            return;
        }

        YamlConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        // Get the current time rounded to the nearest half hour
        Timestamp roundedTimestamp = roundToNearestHalfHour(new Timestamp(System.currentTimeMillis()));

        // Build the SQL query dynamically with stock columns and values
        StringBuilder query = new StringBuilder("INSERT INTO stock_history (recorded_at");

        // Loop through all the stocks in Prices.yml
        for (String stockName : pricesConfig.getConfigurationSection("Stocks").getKeys(false)) {
            query.append(", ").append(stockName);
        }
        query.append(") VALUES (?, ");
        for (int i = 0; i < pricesConfig.getConfigurationSection("Stocks").getKeys(false).size(); i++) {
            query.append("?, ");
        }
        query.setLength(query.length() - 2);  // Remove the last comma and space
        query.append(")");

        try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
            // Set the recorded timestamp for all stocks
            pstmt.setTimestamp(1, roundedTimestamp);

            // Set the stock prices dynamically
            int index = 2;
            for (String stockName : pricesConfig.getConfigurationSection("Stocks").getKeys(false)) {
                // Fetch the latest price from Prices.yml
                int latestPrice = pricesConfig.getInt("Stocks." + stockName + ".Price");
                pstmt.setInt(index++, latestPrice);
            }

            pstmt.executeUpdate();
            getLogger().info("Added stock history for all stocks with prices from Prices.yml at " + roundedTimestamp);

        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error adding stock history for all stocks from Prices.yml", e);
        }
    }

    private Timestamp roundToNearestHalfHour(Timestamp timestamp) {
        long timeInMillis = timestamp.getTime();
        long interval = 1000 * 60 * 30; // 30 minutes in milliseconds

        // Calculate the remainder when dividing by the interval
        long remainder = timeInMillis % interval;

        // If remainder is greater than or equal to half of the interval, round up
        if (remainder >= interval / 2) {
            timeInMillis += (interval - remainder);  // Round up to the next half-hour
        } else {
            timeInMillis -= remainder;  // Round down to the previous half-hour
        }

        return new Timestamp(timeInMillis);
    }

    public void CheckForNewCurrencies() {
        List<String> stocks = CryptoFrenzy.AvailableStocks();

        try (Statement stmt = connection.createStatement()) {
            // Loop through the available stocks
            for (String stock : stocks) {
                // Check if the stock column already exists
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(stock_history)");
                boolean hasStockColumn = false;
                while (rs.next()) {
                    if (stock.equals(rs.getString("name"))) {
                        hasStockColumn = true;
                        break;
                    }
                }

                // If the stock column is missing, add it to the table
                if (!hasStockColumn) {
                    String alterTableQuery = "ALTER TABLE stock_history ADD COLUMN " + stock + " INTEGER DEFAULT 0";
                    stmt.executeUpdate(alterTableQuery);
                    getLogger().info("Added missing '" + stock + "' column to the stock_history table.");
                }
            }

            // Round the timestamp after checking for new stocks
            Timestamp roundedTimestamp = roundToNearestHalfHour(new Timestamp(System.currentTimeMillis()));
            System.out.println("Rounded timestamp for stock update: " + roundedTimestamp);

        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error checking for new currencies", e);
        }
    }

    public void createTableIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            // Create table if it doesn't exist
            String createTableQuery = "CREATE TABLE IF NOT EXISTS stock_history ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "recorded_at DATETIME)";
            stmt.executeUpdate(createTableQuery);

            List<String> stocks = CryptoFrenzy.AvailableStocks();

            for (String stock : stocks) {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(stock_history)");
                boolean hasStockColumn = false;
                while (rs.next()) {
                    if (stock.equals(rs.getString("name"))) {
                        hasStockColumn = true;
                        break;
                    }
                }

                if (!hasStockColumn) {
                    String alterTableQuery = "ALTER TABLE stock_history ADD COLUMN " + stock + " INTEGER DEFAULT 0";
                    stmt.executeUpdate(alterTableQuery);
                    getLogger().info("Added missing '" + stock + "' column to the stock_history table.");
                }
            }

            // Round the timestamp after table creation
            Timestamp roundedTimestamp = roundToNearestHalfHour(new Timestamp(System.currentTimeMillis()));
            System.out.println("Rounded timestamp for table creation: " + roundedTimestamp);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getStock1hPrice(String stock) {
        String query = "SELECT " + stock + " FROM stock_history WHERE recorded_at >= ? ORDER BY recorded_at DESC LIMIT 1";
        return getStockPrice(query, 1000 * 60 * 60);  // 1 hour in milliseconds
    }

    public double getStock24hPrice(String stock) {
        String query = "SELECT " + stock + " FROM stock_history WHERE recorded_at >= ? ORDER BY recorded_at DESC LIMIT 1";
        return getStockPrice(query, 1000 * 60 * 60 * 24);  // 24 hours in milliseconds
    }

    public double getStock7dPrice(String stock) {
        String query = "SELECT " + stock + " FROM stock_history WHERE recorded_at >= ? ORDER BY recorded_at DESC LIMIT 1";
        return getStockPrice(query, 1000 * 60 * 60 * 24 * 7);  // 7 days in milliseconds
    }

    private double getStockPrice(String query, long timeInterval) {
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            // Set the timestamp for the requested time interval
            long timeThreshold = System.currentTimeMillis() - timeInterval;
            pstmt.setTimestamp(1, new Timestamp(timeThreshold));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);  // Return the price from the column
                }
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error retrieving stock price", e);
        }
        return -1;  // Return -1 if no price is found
    }

    public void removeOldHistory() {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

        long fourteenDays = 1000L * 60 * 60 * 24 * 14;
        Timestamp thresholdTimestamp = new Timestamp(currentTimestamp.getTime() - fourteenDays);

        String query = "DELETE FROM stock_history WHERE recorded_at < ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setTimestamp(1, thresholdTimestamp);
            int rowsAffected = pstmt.executeUpdate();

            getLogger().info("Removed " + rowsAffected + " records older than 8 days.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error removing old stock history", e);
        }
    }
}
