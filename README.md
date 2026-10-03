# UltimateEnderChest

**Your Ender Chest, anywhere — with twice the space.**

UltimateEnderChest is a lightweight plugin for **Paper 26.2** that opens a personal **54-slot Ender Chest** with `/enderchest` or `/ec`. The command takes you straight to a six-row inventory, just like a double chest: no menus, buttons, or physical Ender Chest required.

## Features

- **54 slots:** six rows of nine, twice the vanilla capacity.
- **Access anywhere:** use `/enderchest` or its short alias `/ec`.
- **Private storage:** each player can access only their own chest.
- **Existing items preserved:** the first 27 slots are your original vanilla Ender Chest.
- **Physical chest support:** opening an Ender Chest block also opens the same 54-slot inventory.
- **Persistent storage:** extra slots are stored in Minecraft player data, with no external database.
- **Available to everyone by default:** no operator status required.
- **No client mods or additional plugins required.**

## Requirements

| Requirement | Supported version |
| --- | --- |
| Server | Paper 26.2 |
| Java | 25 |
| Client mods | None |
| Plugin dependencies | None |

Other server versions and Folia are not currently tested or supported.

## Installation

1. Download `UltimateEnderChest-1.1.0.jar` from the repository's **Releases** section once a release is available, or build it from source below.
2. Stop your server.
3. Place the JAR in the server's `plugins` folder.
4. Start the server and run `/enderchest` in game.

No configuration is required.

### Upgrading from EnderChestPlus

1. Stop the server and back up your world, including player data.
2. Remove the old `EnderChestPlus-1.0.0.jar` from `plugins`.
3. Add `UltimateEnderChest-1.1.0.jar` and start the server.

**Do not run both plugins at the same time.** UltimateEnderChest reads the original extra-slot data and migrates it automatically when the chest is saved. Your existing items remain available in all 54 slots.

If you configured permissions or command aliases for the previous plugin, update them to the new names shown below. After migration, use a backup if you need to return to the old plugin: EnderChestPlus cannot read the new extra-slot key.

## Commands

| Command | Description |
| --- | --- |
| `/enderchest` | Open your personal 54-slot Ender Chest. |
| `/ec` | Short alias for `/enderchest`. |
| `/ultimateenderchest:enderchest` | Explicit command if another plugin uses the same name. |

These commands open only your own inventory and do not accept player names or other arguments.

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `ultimateenderchest.use` | Open your own Ender Chest using the command. | Everyone |

You can deny this permission through your permission manager to restrict remote access. Opening a physical Ender Chest remains available through normal Minecraft interaction.

## Storage and backups

The first 27 slots remain in the vanilla Ender Chest. The additional 27 slots are serialized with Paper's item API and stored in the player's persistent data container, under `ultimateenderchest:extra_slots_v1`.

Changes are saved when the inventory closes, when the player disconnects, and when the plugin shuts down. Click and drag changes also trigger a save after 10 server ticks, approximately half a second at 20 TPS.

Include your world's **player data** in backups; backing up only the `plugins` folder will not back up chest contents. An abrupt server crash can lose changes that have not yet been saved.

### Removing the plugin

Removing UltimateEnderChest restores access to the normal 27-slot Ender Chest. The additional slots remain in player data and become accessible again when you reinstall UltimateEnderChest. Retrieve items from the extra rows before permanently uninstalling it.

## Command conflicts

If another plugin already handles `/enderchest` or `/ec`, use `/ultimateenderchest:enderchest`. To make the shorter commands point to this plugin, merge the following aliases into the server's `commands.yml`, then restart:

```yaml
aliases:
  enderchest:
    - "ultimateenderchest:enderchest $1-"
  ec:
    - "ultimateenderchest:enderchest $1-"
```

Avoid running multiple plugins that replace the Ender Chest interface or modify its contents while it is open. Compatibility with third-party chest and protection plugins must be checked on your server. UltimateEnderChest respects native inventory-opening events cancelled before its handler runs.

## Build from source

Install **JDK 25** and **Maven 3.9+**, then run this command in the repository root:

```sh
mvn clean package
```

The compiled plugin will be available at:

```text
target/UltimateEnderChest-1.1.0.jar
```

Paper and the test framework are build-time dependencies and are not bundled into the plugin JAR.

## Testing

The project includes automated tests using MockBukkit for Paper 26.2, covering inventory size, command access, vanilla item preservation, player isolation, serialization, saving, and migration from EnderChestPlus.

```sh
mvn test
```

The original EnderChestPlus version was reported working on a real Paper server. Automated tests simulate server behavior; the renamed version's full Minecraft client interaction and Paper's player-data disk writes still need an in-game check after upgrading.

## Reporting an issue

Include your Paper version, Java version, plugin version, relevant server logs, and steps to reproduce the problem. Mention any other plugins that handle Ender Chests or the same commands. Do not include private player data or credentials.
