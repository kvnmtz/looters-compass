# 🧭 Looter's Compass

Makes finding loot containers from the [Lootr](https://modrinth.com/mod/lootr) mod easier. No more missing out on nearby loot :D

## 🆕 What does it add?

![Looter's Compass](docs/looters_compass.webp)

This mod adds a new item called the **Looter's Compass**.
It points to the nearest **loot containers** from the Lootr mod, which has **not yet been opened** by the player.
It has a maximum search radius of 32 blocks horizontally and 16 blocks vertically by default (configurable).
It works with any kind of Lootr's loot containers (chests, barrels, shulker boxes, chest minecarts ...).
When right-clicking with the Looter's Compass in the player's hands, the currently targeted loot container will be **highlighted** (like the Glowing effect does it) for a few seconds.
If no container is found, an idle animation will be shown.
Searching for the loot containers happens on the client-side, which means that server performance will not be hurt in any way.

## ⚙️ Configuration

### Client

The client config file is located in `.minecraft/config/looters_compass-client.toml`.
It contains the following option:

| Option             | Description                                        | Default |
|--------------------|----------------------------------------------------|---------|
| `search_frequency` | How often to search for loot containers (in ticks) | `20`    |

### Common

The common config file is located in:

| Client                                          | Server                                               |
|-------------------------------------------------|------------------------------------------------------|
| `.minecraft/config/looters_compass-common.toml` | `<server_folder>/config/looters_compass-common.toml` |

It contains the following options:

| Option                                  | Description                                                                                                                                                       | Default |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `add_to_centennial_advancement_rewards` | Whether the Looter's Compass should also be rewarded to the player when Lootr's "Centennial" advancement is made (when the player opens the 100th loot container) | `false` |
| `disable_recipe`                        | Whether the crafting recipe for the Looter's Compass should be disabled                                                                                           | `false` |

### Server

The server config file is located in:

| Client                                                                   | Server                                                           |
|--------------------------------------------------------------------------|------------------------------------------------------------------|
| `.minecraft/saves/<world_name>/serverconfig/looters_compass-server.toml` | `<server_folder>/world/serverconfig/looters_compass-server.toml` |

It contains the following options:

| Option                       | Description                                                                                     | Default |
|------------------------------|-------------------------------------------------------------------------------------------------|---------|
| `search_radius_horizontal`   | Horizontal search radius for finding loot containers (in blocks)                                | `32`    |
| `search_radius_vertical`     | Vertical search radius for finding loot containers (in blocks)                                  | `16`    |
| `enable_loot_container_glow` | Whether to enable the ability to make loot containers glow when right-clicking with the compass | `true`  |
| `glow_cooldown`              | Cooldown for the glow feature in ticks (20 ticks = 1 second)                                    | `300`   |
| `glow_duration`              | Duration for how long loot containers should glow in seconds                                    | `5`     |

## 💡 Tips for modpack makers

I think it's a good idea to incentivize exploration in the early game by disabling the crafting recipe for the Looter's Compass using the config option or something like KubeJS.
You could then include a quest which rewards the compass when the player gets the "Centennial" achievement from Lootr (which requires the player to open 100 loot containers first), or just enable the config option that automatically rewards the compass when this advancement is made.