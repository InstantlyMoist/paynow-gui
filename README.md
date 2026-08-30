# PayNow GUI

An in-game store GUI for Minecraft servers, backed by the
[PayNow](https://paynow.gg) storefront API.

Runs on **Bukkit/Spigot/Paper, Fabric and NeoForge**, from Minecraft **1.8 through 26.2**.

## Supported platforms

| Platform | Minecraft | Notes |
|---|---|---|
| Bukkit / Spigot / Paper | 1.8 → 26.2 | single jar, Java 8 bytecode |
| Fabric | 1.20.5 → 26.2 | 18 builds |
| NeoForge | 1.20.6 → 26.2 | 11 builds |

Fabric and NeoForge builds are server-side only — players connect with a vanilla client.

### Feature differences

Some integrations only exist on Bukkit:

| Feature | Bukkit | Fabric / NeoForge |
|---|---|---|
| Store GUI, cart, checkout link | yes | yes |
| Lunar Client embedded checkout (Apollo) | yes | no — Apollo has no Fabric/NeoForge platform |
| Recent-donator NPC (Citizens / SpaceNPC) | yes | no |
| Custom model data | 1.14+ | yes |

## Installing

1. Drop the jar for your platform into `plugins/` (Bukkit) or `mods/` (Fabric/NeoForge).
2. Start the server once to generate the config.
3. Set `store_identifier` in the config, then restart.
   * Bukkit: `plugins/paynow-gui/config.yml`
   * Fabric/NeoForge: `config/paynow-gui/config.yml`
4. Restart again to generate `products.yml`.

Both `config.yml` and `products.yml` are yours to edit — `products.yml` controls how each
product is displayed in the GUI.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/buy` | — | opens the store GUI |
| `/buy reload` | `paynowgui.reload` (Bukkit) / op level 2 | reloads config and products |

In the product view: **left-click** adds one, **right-click** removes one,
**shift + right-click** clears that product from the cart.

## Building

See [BUILDING.md](BUILDING.md). Note that the PayNow SDK is not published publicly and
must be built locally first.

## Support

Join the [Discord](https://kyllian.nl/discord).
