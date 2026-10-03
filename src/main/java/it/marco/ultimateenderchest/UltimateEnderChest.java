package it.marco.ultimateenderchest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** The first 27 slots remain vanilla; the extension lives in the same player data. */
public class UltimateEnderChest extends JavaPlugin implements Listener {
    private NamespacedKey extraKey;
    private NamespacedKey legacyExtraKey;
    private final Map<UUID, Chest> openChests = new HashMap<>();
    private boolean stopping;

    @Override public void onEnable() {
        extraKey = new NamespacedKey(this, "extra_slots_v1");
        legacyExtraKey = NamespacedKey.fromString("enderchestplus:extra_slots_v1");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("enderchest").setExecutor(this);
        getCommand("enderchest").setTabCompleter((sender, command, alias, args) -> java.util.List.of());
        getLogger().info("UltimateEnderChest enabled: /enderchest, 54 personal slots.");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
        } else if (args.length != 0) {
            player.sendMessage("Use /enderchest to open your Ender Chest.");
        } else if (player.hasPermission("ultimateenderchest.use")) {
            open(player);
        }
        return true;
    }

    private void open(Player player) {
        if (stopping || !player.isOnline() || player.isDead()) return;
        Inventory current = player.getOpenInventory().getTopInventory();
        if (current != null && current.getHolder() instanceof Chest) return;
        // Closing first commits any previous inventory before reading the vanilla slots.
        player.closeInventory();
        Chest retained = openChests.get(player.getUniqueId());
        if (retained != null) {
            player.openInventory(retained.inventory);
            return;
        }
        try {
            byte[] data = player.getPersistentDataContainer().get(extraKey, PersistentDataType.BYTE_ARRAY);
            // Read the original plugin's namespace without changing or discarding its data.
            if (data == null) data = player.getPersistentDataContainer().get(legacyExtraKey, PersistentDataType.BYTE_ARRAY);
            ItemStack[] extra = data == null ? new ItemStack[27] : ItemStack.deserializeItemsFromBytes(data);
            if (extra.length != 27) throw new IllegalStateException("Invalid saved slot count: " + extra.length);
            Chest chest = new Chest(player);
            ItemStack[] items = new ItemStack[54];
            ItemStack[] original = player.getEnderChest().getContents();
            for (int i = 0; i < 27; i++) {
                items[i] = copy(original[i]);
                items[i + 27] = copy(extra[i]);
            }
            chest.inventory.setContents(items);
            openChests.put(player.getUniqueId(), chest);
            player.openInventory(chest.inventory);
            // Another plugin can reject the opening; do not keep a stale session.
            if (player.getOpenInventory().getTopInventory() != chest.inventory) {
                openChests.remove(player.getUniqueId(), chest);
            }
        } catch (RuntimeException error) {
            getLogger().log(Level.SEVERE, "Cannot read Ender Chest for " + player.getUniqueId(), error);
            player.sendMessage("Unable to open your Ender Chest. Please contact an administrator.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNativeOpen(InventoryOpenEvent event) {
        if (stopping || event.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(this, () -> open(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Chest chest) scheduleSave(chest);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Chest chest) scheduleSave(chest);
    }

    private void scheduleSave(Chest chest) {
        if (stopping || chest.savePending) return;
        chest.savePending = true;
        // Wait until Bukkit has applied the click/drag. Coalesce rapid clicks.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            chest.savePending = false;
            if (openChests.get(chest.player.getUniqueId()) == chest) save(chest);
        }, 10L);
    }

    private boolean save(Chest chest) {
        try {
            ItemStack[] contents = chest.inventory.getContents();
            // Serialize before changing player data, so a codec failure cannot partially commit.
            byte[] extra = ItemStack.serializeItemsAsBytes(Arrays.copyOfRange(contents, 27, 54));
            chest.player.getPersistentDataContainer().set(extraKey, PersistentDataType.BYTE_ARRAY, extra);
            // Replace the legacy key only after successfully encoding the complete extension.
            chest.player.getPersistentDataContainer().remove(legacyExtraKey);
            ItemStack[] original = new ItemStack[27];
            for (int i = 0; i < 27; i++) original[i] = copy(contents[i]);
            chest.player.getEnderChest().setContents(original);
            // Native slots, extension and player inventory are saved together by Paper.
            chest.player.saveData();
            return true;
        } catch (RuntimeException error) {
            getLogger().log(Level.SEVERE, "Cannot save Ender Chest for " + chest.player.getUniqueId(), error);
            chest.player.sendMessage("Unable to save your Ender Chest. Please contact an administrator.");
            // Retain the inventory in memory for retry; never replace it with an empty one.
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Chest chest) {
            if (save(chest)) openChests.remove(chest.player.getUniqueId(), chest);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Chest chest = openChests.get(event.getPlayer().getUniqueId());
        if (chest != null && save(chest)) openChests.remove(event.getPlayer().getUniqueId(), chest);
    }

    @Override public void onDisable() {
        stopping = true;
        for (Chest chest : java.util.List.copyOf(openChests.values())) {
            // Closing also returns any cursor item before saving player inventory.
            chest.player.closeInventory();
            if (openChests.containsKey(chest.player.getUniqueId())) save(chest);
        }
        openChests.clear();
    }

    private static ItemStack copy(ItemStack item) { return item == null ? null : item.clone(); }

    private static final class Chest implements InventoryHolder {
        private final Player player;
        private final Inventory inventory;
        private boolean savePending;
        private Chest(Player player) {
            this.player = player;
            inventory = Bukkit.createInventory(this, 54, Component.text("Ultimate Ender Chest"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }
}

