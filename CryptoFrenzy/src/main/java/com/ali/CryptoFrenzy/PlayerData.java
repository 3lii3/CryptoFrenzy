package com.ali.CryptoFrenzy;

import java.util.logging.Logger;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.List;
import static org.bukkit.Bukkit.getLogger;

public class PlayerData {

    private final CryptoFrenzy plugin;

    private Connection connection;

    // Modify the constructor to accept the plugin as a parameter
    public PlayerData(Connection connection,CryptoFrenzy plugin) {
        this.plugin = plugin;
        this.connection = connection;
    }

    public void createTableIfNotExists() {
        if (connection == null) {
            getLogger().severe("Database connection is not established.");
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS player_data ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "name TEXT)";
            stmt.executeUpdate(createTableQuery);

            List<String> stocks = CryptoFrenzy.AvailableStocks();

            for (String stock : stocks) {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(player_data)");
                boolean hasStockColumn = false;
                while (rs.next()) {
                    if (stock.equals(rs.getString("name"))) {
                        hasStockColumn = true;
                        break;
                    }
                }

                // If the column doesn't exist, add it
                if (!hasStockColumn) {
                    String alterTableQuery = "ALTER TABLE player_data ADD COLUMN " + stock + " INTEGER DEFAULT 0";
                    stmt.executeUpdate(alterTableQuery);
                    getLogger().info("Added missing '" + stock + "' column to the player_data table.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addNewPlayer(String uuid, String name) {
        String insertPlayerSQL = "INSERT INTO player_data (uuid, name) VALUES (?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(insertPlayerSQL)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.executeUpdate();
            getLogger().log(Level.INFO, "New player " + name + " added to the database with UUID: " + uuid);

            List<String> stocks = CryptoFrenzy.AvailableStocks();

            for (String stock : stocks) {
                String updateStockSQL = "UPDATE player_data SET " + stock + " = 0 WHERE uuid = ?";
                try (PreparedStatement updatePS = connection.prepareStatement(updateStockSQL)) {
                    updatePS.setString(1, uuid);
                    updatePS.executeUpdate();
                }
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error adding new player", e);
        }
    }

    public void addCoins(String uuid, String stock, int amount, String playerName) {
        if (!playerExists(uuid)) {
            addNewPlayer(uuid, playerName);  // Add the player if they don't exist
            getLogger().log(Level.INFO, "Player doesn't exist, Adding player.");
        }

        // Now proceed to add coins to the specific stock for the player
        String selectCoinsSQL = "SELECT " + stock + " FROM player_data WHERE uuid = ?";

        try (PreparedStatement ps = connection.prepareStatement(selectCoinsSQL)) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();

            // Check if the player has data for the stock
            if (rs.next()) {
                int currentAmount = rs.getInt(stock);  // Get the current amount of the stock
                int newAmount = currentAmount + amount; // Update the amount

                // Update the stock column with the new amount
                String updateCoinsSQL = "UPDATE player_data SET " + stock + " = ? WHERE uuid = ?";
                try (PreparedStatement updatePS = connection.prepareStatement(updateCoinsSQL)) {
                    updatePS.setInt(1, newAmount);
                    updatePS.setString(2, uuid);
                    updatePS.executeUpdate();
                }
            } else {
                // If the player doesn't have the stock, initialize it with the given amount
                String updateCoinsSQL = "UPDATE player_data SET " + stock + " = ? WHERE uuid = ?";
                try (PreparedStatement updatePS = connection.prepareStatement(updateCoinsSQL)) {
                    updatePS.setInt(1, amount);
                    updatePS.setString(2, uuid);
                    updatePS.executeUpdate();
                    getLogger().log(Level.INFO, "Added "+amount+" of "+stock+" to "+playerName+" of UUID "+uuid);
                }
            }

        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error adding coins", e);
        }
        plugin.reloadPricesConfig();
    }

    // Remove coins from the player's stock.
    public boolean removeCoins(String uuid, String stock, int amount) {
        String selectCoinsSQL = "SELECT " + stock + " FROM player_data WHERE uuid = ?";

        try (PreparedStatement ps = connection.prepareStatement(selectCoinsSQL)) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();

            // Check if the player has the specified stock
            if (rs.next()) {
                int currentAmount = rs.getInt(stock); // Get the current amount of the stock
                if (currentAmount >= amount) {
                    int newAmount = currentAmount - amount; // Decrease the amount

                    // Update the stock column with the new amount
                    String updateCoinsSQL = "UPDATE player_data SET " + stock + " = ? WHERE uuid = ?";
                    try (PreparedStatement updatePS = connection.prepareStatement(updateCoinsSQL)) {
                        updatePS.setInt(1, newAmount);
                        updatePS.setString(2, uuid);
                        updatePS.executeUpdate();
                        getLogger().log(Level.INFO, "Removed "+amount+" of "+stock+" from UUID: "+uuid);
                        plugin.reloadPricesConfig();
                        return true;
                    }
                } else {
                    getLogger().warning("Not enough " + stock + " to remove.");
                    return false;
                }
            } else {
                getLogger().warning("Player not found or stock not available.");
                return false;
            }

        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error removing coins", e);
        }
        plugin.reloadPricesConfig();
        return true;
    }

    // Fetch player's coin inventory.
    public Map<String, Integer> fetchPlayerCoins(String uuid) {
        // Query to select all stocks (columns) for the player
        String selectCoinsSQL = "SELECT * FROM player_data WHERE uuid = ?";
        Map<String, Integer> coins = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(selectCoinsSQL)) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Iterate over all columns (stocks) in the result set
                ResultSetMetaData metaData = rs.getMetaData();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    if (!columnName.equals("uuid") && !columnName.equals("name")) {
                        // Get the value of the stock and put it in the map
                        int stockAmount = rs.getInt(columnName);
                        coins.put(columnName, stockAmount);
                    }
                }
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error fetching player coins", e);
        }
        plugin.reloadPricesConfig();
        return coins;
    }

    public boolean playerExists(String uuid) {
        String checkPlayerSQL = "SELECT COUNT(*) FROM player_data WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkPlayerSQL)) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;  // Return true if the count is greater than 0
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Error checking if player exists", e);
        }
        return false;
    }
}