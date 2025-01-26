![1](https://github.com/user-attachments/assets/6d20fa57-c4bf-48ab-8655-c62205f366c5)

# Minecraft stocks plugin
Add a new way to earn money in your minecraft server!

![2](https://github.com/user-attachments/assets/1821ce8d-0845-436f-8557-686a4e46508e)

## Features:

### Dynamic pricing using Supply & Demand!
Get the best & most realistic system for pricing, Depending 100% on playerbase economy!

### Market Crashes! (BETA)
Random market crashes with prices dropping adding a new flavour to your minecraft server!

### Player investments portfolios!
View other's investments & profits!

### Stock fetching & history!
Check your favourite stock's information to learn more!

## Commands:

### Player commands:
- **/stocks help** Shows help menu
- **/stocks buy <stock> <amount>** Buys stocks.
- **/stocks sell <stock> <amount>** Sells stocks.
- **/stocks portfolio (player)** Shows player's portfolio (or other's).
- **/stocks buy <stock> <amount>** Buys stocks.
- **/stocks send <player> <stock> <amount>** Sends stocks to other players.
- **/stocks fetch <stock>** Shows information about specific stock.

### Admin commands:
- **/stocks reload** Shows help menu
- **/stocks add <player> <stock> <amount>** Adds stocks to a player.
- **/stocks remove <player> <stock> <amount>** Removes stocks from a player.
- **/stocks crash <stock>** Crashes a specific market.

## Permissions:
  
### Player permissions:
- **stocks.buy** Allows player to buy stocks.
- **stocks.sell** Allows player to sell stocks.
- **stocks.send** Allows player to send stocks to players.
- **stocks.portfolio.other** Allows player to check other player portfolio.
- **stocks.portfolio** Allows player to check their portfolio.
- **stocks.help** Allows player to see help menu.
- **stocks.fetch** Allows player to view information about a stock. 

### VIP permissions:
- **stocks.cooldown** Allows player to skip command cooldown.

### Admin permissions:
- **stocks.remove** Allows player to remove stocks from player.
- **stocks.add** Allows player to add stocks to player.
- **stocks.reload** Allows player to reload configurations.
- **stocks.crash** Allows player to crash markets.

![3](https://github.com/user-attachments/assets/11293086-10c4-404e-bd00-2cb7fb67ecf3)

## Default Configuration:
<details>
  <summary>View config</summary>
  
  ```yaml
  # CryptoFrenzy Plugin Configuration

#Check for updates when enabling plugin?
notifyUpdate: true

# Player configuration
player:
  cooldown:
    # Shall cooldown be enabled? Default: true
    # Advice: Enable the cooldown to avoid a lot of database requests and better performance.
    enabled: true
    # Time for cooldown in seconds (if enabled) Default: 5 seconds.
    time: 5

# How frequent should the database update in minutes (Price history only, Don't set below 30 for best performance.)
DatabaseUpdateFrequency: 30
## The stock database is only used to fetch for history like price before 1h, 24h, 7d and not anything extra.

# Economy configuration
economy:
  #Tax to get from player for each share, 1 is 1% / Default: 1.0
  tax-rate: 1.0
  #Amount to deduct from player's balance for every transaction (leave 0 to disable):
  market-fee: 0.0

# Random events configuration
Events:
  #Randomly crash the market every few days
  market-crash:
    enabled: true
    # How frequent should the crashes be? (Format: "1d" = 1 day, "12h" = 12 hours)
    crash-frequency: "16h"
    # ^ Formats:
    # d (day)
    # h (hour)

    # Warn players before crashing market?
    crash-warning: true
    # Broadcast crashes?
    crash-broadcast: true
    # minimum % for a market to go down
    min-rate: 15
    # maximum % for a market to go down
    max-rate: 25
    # Should the /market crash command be enabled?
    command-enabled: true
    # Recovery options
    market-recovery:
      enabled: true
      # minimum % for a market to recover
      min-rate: 25
      # maximum % for a market to recover
      max-rate: 60
      # After for many minutes shall the market recover? (Recovery rate is linear)
      recover-duration: 10
      # (Recovery in 10 minutes is fast, 120 minutes is slow)

      # Shall server broadcast a recovery message after a market crash?
      recovery-broadcast: true

      # Recovery / crash logic:
      # Crash rate is between min-max-rates, So if it's 50, and demand was 1000 it drops to 500 in demand, causing prices to go down.
      # And if recovery rate is 25, It will be 25% of 500(Crashed amount) which is 125, So demand will go up to 625.

# Stocks configuration (You can add stocks here)
Stocks:
  #Make sure to have all stock shortnames in uppercase!

  #format:

  #stock:
    #stock-name: "Stock"
    #description: "Default description"
    #price: 10000
    #max-shares: 10
    #market-shares: 100000

  CFZ:
    currency-name: "CryptoFrenzy coin"
    description: "A basic coin."
    #Starting price for one stock
    price: 100
    #Max shares owned by a single player
    max-shares: 100
    #Shares circulating in the market
    market-shares: 100000
  SVR:
    currency-name: "Server Coin"
    description: "Server's own stocks."
    #Starting price for one stock
    price: 120
    #Max shares owned by a single player
    max-shares: 100
    #Shares circulating in the market (Should be more than server's shares)
    market-shares: 100000
  MINC:
    currency-name: "Minecraft coin"
    description: "Minecraft's coins."
    #Starting price for one stock
    price: 60
    #Max shares owned by a single player
    max-shares: 100
    #Shares circulating in the market
    market-shares: 100000
  ```
  
</details>
