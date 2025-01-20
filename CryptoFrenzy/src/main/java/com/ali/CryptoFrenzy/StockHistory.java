package com.ali.CryptoFrenzy;

import java.sql.*;
import java.util.logging.Level;
import java.util.List;
import static org.bukkit.Bukkit.getLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class StockHistory {

    private Connection connection;
    final private CryptoFrenzy plugin;

    public StockHistory(Connection connection, CryptoFrenzy plugin) {
        this.plugin = plugin;
        this.connection = connection;
    }

    public void updateAllStockHistory() {
        File pricesFile = new File(plugin.getDataFolder(), "Prices.yml");
        if (!pricesFile.exists()) {
            getLogger().warning("Prices.yml does not exist. Unable to update stock history.");
            return;
        }

        YamlConfiguration pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        Timestamp roundedTimestamp = roundToNearestHalfHour(new Timestamp(System.currentTimeMillis()));

        StringBuilder query = new StringBuilder("INSERT INTO stock_history (recorded_at");

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
            pstmt.setTimestamp(1, roundedTimestamp);

            int index = 2;
            for (String stockName : pricesConfig.getConfigurationSection("Stocks").getKeys(false)) {
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
        long interval = 1000 * 60 * 30;

        long remainder = timeInMillis % interval;

        if (remainder >= interval / 2) {
            timeInMillis += (interval - remainder);
        } else {
            timeInMillis -= remainder;
        }

        return new Timestamp(timeInMillis);
    }

    public void CheckForNewCurrencies() {
        List<String> stocks = CryptoFrenzy.AvailableStocks();

        try (Statement stmt = connection.createStatement()) {
            for (String stock : stocks) {
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

            Timestamp roundedTimestamp = roundToNearestHalfHour(new Timestamp(System.currentTimeMillis()));
            System.out.println("Rounded timestamp for stock update: " + roundedTimestamp);

        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error checking for new currencies", e);
        }
    }

    public void createTableIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            // Create table if it doesn't exist (for SQLite, using AUTOINCREMENT)
            String createTableQuery = "CREATE TABLE IF NOT EXISTS stock_history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
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

            getLogger().info("Removed " + rowsAffected + " records older than 14 days.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error removing old stock history", e);
        }
    }
}