![UltimateEnderChest — 54 slots. Anywhere.](assets/banner.png)

# UltimateEnderChest

**A Minecraft plugin for Paper that lets you open your own Ender Chest anywhere — with 54 slots instead of 27.**

Type **`/enderchest`** or **`/ec`** to open it instantly. You get six rows of storage, just like a double chest, without needing an Ender Chest block nearby.

Your items stay private, your existing vanilla items remain available, and your storage is saved between server restarts.

**[Download the plugin](https://github.com/Samu3k/UltimateEnderChest/releases/latest)** · **[Report an issue](https://github.com/Samu3k/UltimateEnderChest/issues)**

## What you get

- **Twice the space:** 54 slots, arranged in six rows of nine.
- **Access anywhere:** one command takes you straight to your chest.
- **Your own storage:** every player has a separate inventory.
- **Your existing items:** the original 27 slots become the first three rows.
- **The same chest everywhere:** opening a physical Ender Chest also shows all 54 slots.
- **Simple setup:** no configuration, extra plugins, or client mods required.

## Install in three steps

**Requires Paper 26.2 and Java 25.**

1. Stop your server.
2. Put `UltimateEnderChest-1.1.0.jar` in the server's `plugins` folder.
3. Start the server and type `/enderchest` in game.

The command is available to everyone by default, including players without operator status.

## Technical details

### Compatibility

| Requirement | Version |
| --- | --- |
| Server | Paper 26.2 |
| Java | 25 |
| Plugin dependencies | None |
| Client mods | None |

Other Minecraft versions and Folia are not currently tested or supported.

### Commands and permission

| Command | Action |
| --- | --- |
| `/enderchest` | Open your own 54-slot Ender Chest. |
| `/ec` | Short alias for `/enderchest`. |
| `/ultimateenderchest:enderchest` | Use this plugin explicitly if another plugin handles the same command. |

Commands do not accept player names or other arguments.

**Permission:** `ultimateenderchest.use` — enabled for everyone by default. Deny it through your permission manager to restrict command access. Opening a physical Ender Chest remains available through normal Minecraft interaction.

### Storage and backups

The first 27 slots remain in the vanilla Ender Chest. The additional 27 slots are serialized with Paper's item API and stored in the player's persistent data container, under `ultimateenderchest:extra_slots_v1`. No external database is needed.

Changes are saved when the inventory closes, the player disconnects, or the plugin shuts down. Clicks and drags also trigger a save after 10 server ticks, approximately half a second at 20 TPS.

Back up your world's **player data**, not just the `plugins` folder. An abrupt crash can lose changes that have not yet been saved.

### Upgrading from EnderChestPlus

Stop the server, back up your world and player data, remove `EnderChestPlus-1.0.0.jar`, and install the new JAR. **Do not run both plugins together.**

Existing extra-slot items are read automatically and migrated when the chest is saved. Update any configured permissions and command aliases to the new plugin name. After migration, returning to EnderChestPlus requires a backup because the old plugin cannot read the new storage key.

### Uninstalling

Without this plugin, players can access only the original 27 slots. Extra-slot items remain in player data and become accessible again when UltimateEnderChest is reinstalled. Retrieve items from the extra rows before permanently uninstalling.

### Command conflicts

If `/enderchest` or `/ec` opens another plugin's inventory, use `/ultimateenderchest:enderchest`. To assign the shorter commands to UltimateEnderChest, merge these aliases into the server's `commands.yml` and restart:

```yaml
aliases:
  enderchest:
    - "ultimateenderchest:enderchest $1-"
  ec:
    - "ultimateenderchest:enderchest $1-"
```

Avoid running multiple plugins that replace Ender Chest inventories or modify their contents while open. Third-party chest and protection plugin compatibility needs to be checked on your server. Native inventory-opening events cancelled before this plugin's handler runs are respected.

### Building and testing

With **JDK 25** and **Maven 3.9+**, run:

```sh
mvn clean package
```

The plugin is created at `target/UltimateEnderChest-1.1.0.jar`. Paper and the test framework are not bundled into the JAR.

**12 automated tests passed**, covering inventory size, commands, vanilla items, player isolation, serialization, saving, and migration from EnderChestPlus. Run them separately with `mvn test`.

Tests use MockBukkit for Paper 26.2. The original EnderChestPlus version was reported working on a real Paper server; the renamed version's Minecraft client interaction and Paper's player-data disk writes still need an in-game check after upgrading.

### Getting help

When reporting an issue, include your Paper, Java, and plugin versions, relevant logs, and steps to reproduce it. Mention any plugins that manage Ender Chests or the same commands. Remove credentials and private player data from attachments.
