package com.ali.CryptoFrenzy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Recovery / crash logic:
// Crash rate is between min-max-rates, So if it's 50, and demand was 1000 it drops to 500 in demand, causing prices to go down.
// And if recovery rate is 25, It will be 25% of 500(Crashed amount) which is 125, So demand will go up to 625.

public class MarketCrashEvent {

    private CryptoFrenzy plugin;
    private PricingHandler pricingHandler;

    public MarketCrashEvent(CryptoFrenzy plugin) {
        this.plugin = plugin;
        this.pricingHandler = new PricingHandler(plugin);

        boolean MarketCrash = plugin.getConfig().getBoolean("Events.market-crash.enabled");
        if (MarketCrash) {
            scheduleNextCrash();
        }
    }

    public void scheduleNextCrash() {
        long delayInTicks = getHours() * 60 * 60 * 20L;

        new BukkitRunnable() {
            @Override
            public void run() {
                triggerCrash(randomStock());
                scheduleNextCrash();
            }
        }.runTaskLater(plugin, delayInTicks);
    }

    public void triggerCrash(String stockName) {
        boolean MarketCrash = plugin.getConfig().getBoolean("Events.market-crash.enabled");
        if (MarketCrash) {
            plugin.getLogger().info("Crashing market for stock " + stockName);
            FileConfiguration config = plugin.getConfig();
            String path = "Events.market-crash.";

            boolean warningEnabled = config.getBoolean(path + "crash-warning");
            if (warningEnabled) {
                broadcastWarning();
                // wait for 30 seconds if warning is enabled
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        proceedWithCrash(stockName);
                    }
                }.runTaskLater(plugin, 300L); // 300L = 15 seconds (in ticks)
            } else {
                proceedWithCrash(stockName);
            }
        }
    }

    private void proceedWithCrash(String stockName) {
        FileConfiguration pricesConfig = plugin.getPricesConfig();
        FileConfiguration config = plugin.getConfig();
        String path = "Events.market-crash.";

        double minRate = config.getDouble(path+"min-rate");
        double maxRate = config.getDouble(path+"max-rate");

        double crashRate = (minRate + (Math.random() * (maxRate - minRate))) / 100;

        int demand = pricesConfig.getInt("Stocks." + stockName + ".market-shares");

        int crashAmount = (int) Math.round((double) demand * crashRate);

        boolean crash = pricingHandler.adjustPriceBasedOnSupplyDemand(stockName, crashAmount, false);

        double newPrice = pricingHandler.getPrice(stockName);

        boolean broadcastEnabled = config.getBoolean(path + "crash-broadcast");

        plugin.reloadPricesConfig();
        pricingHandler.Reload();

        while (crash) {
            plugin.getLogger().info(stockName + " crashed to " + demand + " in demand (Lost "+ crashAmount+" in demand)");
            crash = false;
        }

        if (broadcastEnabled) {
            broadcastCrash(stockName, newPrice);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                recoverCrash(stockName, crashAmount);
            }
        }.runTaskLater(plugin, 600L); // Wait for 30 seconds til recovery
    }

    private String randomStock() {
        FileConfiguration pricesConfig = plugin.getPricesConfig(); // or plugin.getConfig() if you want to fetch from config
        Set<String> stocks = pricesConfig.getConfigurationSection("Stocks").getKeys(false);

        // Convert the Set of stock names to a List for easier access
        List<String> stockList = new ArrayList<>(stocks);

        // Generate a random index to pick a stock
        Random random = new Random();
        int randomIndex = random.nextInt(stockList.size());

        // Get the random stock name
        String stock = stockList.get(randomIndex);

        return stock;
    }

    private void broadcastWarning() {
        plugin.getServer().broadcastMessage(ChatColor.RED+"⚠ Attention! "+ChatColor.YELLOW +" A mysterious market will crash in 15 seconds! Prepare your pockets!");
        // play warning sound effect for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
    }
    private void broadcastCrash(String stockName, Double newPrice) {
        plugin.getServer().broadcastMessage(ChatColor.RED+"⚠ Attention! "+ChatColor.YELLOW + stockName + " Market has crashed! New price: $" + newPrice);
        // play crash sound effect for all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }
    }

    private long getHours() {
        String marketCrash = plugin.getConfig().getString("Events.market-crash.crash-frequency");
        long hours = 0;

        if (marketCrash != null && !marketCrash.isEmpty()) {
            String numberPart = marketCrash.substring(0, marketCrash.length() - 1); // The number part
            char unit = marketCrash.charAt(marketCrash.length() - 1); // The unit part, Either d for day or h for hour

            try {
                long timeValue = Long.parseLong(numberPart.trim());

                if (unit == 'd') {
                    hours = timeValue * 24; // Convert days to hours
                } else if (unit == 'h') {
                    hours = timeValue;
                } else {
                    plugin.getLogger().warning("Invalid unit for market crash frequency: " + unit);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid number format in crash-frequency: " + marketCrash);
            }
        } else {
            return 7;
        }

        return hours;
    }

    private void recoverCrash(String stock, Integer crashAmount) {
        FileConfiguration config = plugin.getConfig();
        String path = "Events.market-crash.market-recovery.";

        boolean recoveryEnabled = config.getBoolean(path + "enabled");
        boolean broadcastEnabled = config.getBoolean(path + "recovery-broadcast");

        int recoveryDuration = config.getInt(path + "recover-duration");

        if (recoveryEnabled) {
            double minRate = config.getDouble(path + "min-rate");
            double maxRate = config.getDouble(path + "max-rate");

            double recoveryRate = (minRate + (Math.random() * (maxRate - minRate))) / 100;
            double totalRecoveryAmount = crashAmount * recoveryRate;

            long recoveryPerMinute = Math.round(totalRecoveryAmount / recoveryDuration);
            long amount = Math.round(totalRecoveryAmount / recoveryDuration);

            plugin.getLogger().info(stock + " market recovery is in process. Total recovery: "+totalRecoveryAmount+" | Recovery increment: "+amount);
            plugin.getLogger().info("Total Recovery Amount: " + totalRecoveryAmount);
            plugin.getLogger().info("Recovery Per Minute: " + recoveryPerMinute);
            plugin.getLogger().info("Amount: " + amount);

            new BukkitRunnable() {
                int minutesPassed = 0;

                @Override
                public void run() {
                    if (minutesPassed < recoveryDuration) {
                        pricingHandler.adjustPriceBasedOnSupplyDemand(stock, (int) amount, true);

                        minutesPassed ++;
                    } else {
                        cancel();
                        plugin.getLogger().info(stock + " market recovery complete.");

                        if (broadcastEnabled) {
                            broadcastRecovery(stock, Math.round(recoveryRate * 100));
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 60 * 20L);
        }
    }

    private void broadcastRecovery(String stockName, double recoverRate) {
        plugin.getServer().broadcastMessage(ChatColor.YELLOW+"⚠ Attention! "+ChatColor.GREEN+"Market "+ChatColor.YELLOW+stockName+ChatColor.GREEN+" has recovered "+ChatColor.YELLOW+recoverRate+"%"+ChatColor.GREEN+" from last crash!");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }
    }
}