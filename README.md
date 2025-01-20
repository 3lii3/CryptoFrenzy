![1](https://github.com/user-attachments/assets/6d20fa57-c4bf-48ab-8655-c62205f366c5)

# Minecraft stocks plugin
Add a new way to earn money in your minecraft server!

![2](https://github.com/user-attachments/assets/1821ce8d-0845-436f-8557-686a4e46508e)

## Features:

### Dynamic pricing using Supply & Demand!
Get the best & most realistic system for pricing, Depending 100% on playerbase economy!

### Market Crashes!
Random market crashes with prices dropping adding a new flavour to your minecraft server!

### Player investments portfolios!
View other's investments & profits!

### Stock fetching & history!
Check your favourite stock's information to learn more!

## Commands:

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
- **stocks.remove** Allows player to remove stocks from player (ADMIN).
- **stocks.add** Allows player to add stocks to player (ADMIN).
- **stocks.reload** Allows player to reload configurations.

![3](https://github.com/user-attachments/assets/11293086-10c4-404e-bd00-2cb7fb67ecf3)

## Default Configuration:
<details>
  <summary>View config</summary>
  
  ```yaml
  # CryptoFrenzy Plugin Configuration

player:
  cooldown:
    # Shall cooldown be enabled? Default: true
    # Advice: Enable the cooldown to avoid a lot of database requests and better performance.
    enabled: true
    # Time for cooldown in seconds (if enabled) Default: 5 seconds.
    time: 5

# How frequent should the database update in minutes (Price history only, Don't set below 30 for best performance.)
DatabaseUpdateFrequency: 30
## The stock database is only used to fetch for history like price before 1hr, 24hrs, 7ds and not anything extra.


economy:
  #Tax to get from player for each share, 1 is 1% / Default: 1
  tax-rate: 1
  #Amount to deduct from player's balance for every transaction (leave 0 to disable):
  market-fee: 2

Events:
  #Randomly crash the market every few days
  market-crash:
    enabled: true
    # minimum % for a market to go down
    min-rate: 10
    # maximum % for a market to go down
    max-rate: 25

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
    market-shares: 10000
  SVR:
    currency-name: "Server Coin"
    description: "Server's own stocks."
    #Starting price for one stock
    price: 120
    #Max shares owned by a single player
    max-shares: 100
    #Shares circulating in the market
    market-shares: 10000
  MINC:
    currency-name: "Minecraft coin"
    description: "Minecraft's coins."
    #Starting price for one stock
    price: 60
    #Max shares owned by a single player
    max-shares: 100
    #Shares circulating in the market
    market-shares: 10000
  ```
  
</details>
