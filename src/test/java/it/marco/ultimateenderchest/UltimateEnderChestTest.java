package it.marco.ultimateenderchest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import static org.junit.jupiter.api.Assertions.*;

class UltimateEnderChestTest {
    ServerMock server;
    UltimateEnderChest plugin;
    SavingPlayer player;
    @BeforeEach void setup() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(UltimateEnderChest.class);
        player = new SavingPlayer(server, "Marco");
        server.addPlayer(player);
    }
    @AfterEach void teardown() { MockBukkit.unmock(); }

    @Test void commandOpensAll54SlotsForNonOperatorAndImportsVanillaItems() {
        player.setOp(false);
        player.getEnderChest().setItem(26, new ItemStack(Material.DIAMOND, 7));
        assertTrue(server.dispatchCommand(player, "enderchest"));
        Inventory chest = player.getOpenInventory().getTopInventory();
        assertEquals(54, chest.getSize());
        assertEquals(new ItemStack(Material.DIAMOND, 7), chest.getItem(26));
        assertNull(chest.getItem(53));
    }

    @Test void bothHalvesSurviveClosingAndReopeningWithoutDuplicating() {
        server.dispatchCommand(player, "enderchest");
        Inventory chest = player.getOpenInventory().getTopInventory();
        chest.setItem(0, new ItemStack(Material.EMERALD, 3));
        chest.setItem(53, new ItemStack(Material.NETHERITE_INGOT, 2));
        player.closeInventory();
        assertTrue(player.saveCalls > 0);
        assertEquals(new ItemStack(Material.EMERALD, 3), player.getEnderChest().getItem(0));
        assertNotNull(player.getPersistentDataContainer().get(new NamespacedKey(plugin, "extra_slots_v1"), PersistentDataType.BYTE_ARRAY));
        server.dispatchCommand(player, "ec");
        Inventory reopened = player.getOpenInventory().getTopInventory();
        assertNotSame(chest, reopened);
        assertEquals(new ItemStack(Material.NETHERITE_INGOT, 2), reopened.getItem(53));
        reopened.setItem(53, null);
        player.closeInventory();
        server.dispatchCommand(player, "ec");
        assertTrue(player.getOpenInventory().getTopInventory().getItem(53) == null || player.getOpenInventory().getTopInventory().getItem(53).getType().isAir());
    }

    @Test void inventoriesArePrivate() {
        server.dispatchCommand(player, "enderchest");
        player.getOpenInventory().getTopInventory().setItem(35, new ItemStack(Material.DIAMOND));
        PlayerMock other = new SavingPlayer(server, "Other");
        server.addPlayer(other);
        server.dispatchCommand(other, "enderchest");
        assertNull(other.getOpenInventory().getTopInventory().getItem(35));
        assertNotSame(player.getOpenInventory().getTopInventory(), other.getOpenInventory().getTopInventory());
    }

    @Test void openingNativeEnderChestRedirectsTo54Slots() {
        player.openInventory(player.getEnderChest());
        server.getScheduler().performTicks(1);
        assertEquals(54, player.getOpenInventory().getTopInventory().getSize());
    }

    @Test void commandCannotOpenAnotherPlayersChest() {
        server.dispatchCommand(player, "enderchest SomeoneElse");
        assertFalse(hasExtendedChest());
    }

    @Test void corruptExtensionDoesNotOpenEmptyChestOrOverwriteData() {
        NamespacedKey key = new NamespacedKey(plugin, "extra_slots_v1");
        byte[] corrupt = new byte[] {1, 2, 3};
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE_ARRAY, corrupt);
        server.dispatchCommand(player, "enderchest");
        assertFalse(hasExtendedChest());
        assertArrayEquals(corrupt, player.getPersistentDataContainer().get(key, PersistentDataType.BYTE_ARRAY));
    }

    @Test void disablingSavesExtraSlotsAndClosesMenu() {
        server.dispatchCommand(player, "enderchest");
        player.getOpenInventory().getTopInventory().setItem(27, new ItemStack(Material.GOLD_INGOT, 6));
        server.getPluginManager().disablePlugin(plugin);
        assertFalse(hasExtendedChest());
        byte[] data = player.getPersistentDataContainer().get(new NamespacedKey(plugin, "extra_slots_v1"), PersistentDataType.BYTE_ARRAY);
        assertEquals(new ItemStack(Material.GOLD_INGOT, 6), ItemStack.deserializeItemsFromBytes(data)[0]);
    }

    @Test void extraItemsSurviveRestoringStoredBytesIntoFreshPlayer() {
        server.dispatchCommand(player, "enderchest");
        player.getOpenInventory().getTopInventory().setItem(53, new ItemStack(Material.DIAMOND, 12));
        player.closeInventory();
        NamespacedKey key = new NamespacedKey(plugin, "extra_slots_v1");
        byte[] savedData = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE_ARRAY).clone();
        SavingPlayer restored = new SavingPlayer(server, "Restored");
        server.addPlayer(restored);
        restored.getPersistentDataContainer().set(key, PersistentDataType.BYTE_ARRAY, savedData);
        server.dispatchCommand(restored, "enderchest");
        assertEquals(new ItemStack(Material.DIAMOND, 12), restored.getOpenInventory().getTopInventory().getItem(53));
    }

    @Test void clickSavesAfterBukkitAppliesTheInventoryChange() {
        server.dispatchCommand(player, "enderchest");
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER, 53, ClickType.LEFT, InventoryAction.PLACE_ALL);
        server.getPluginManager().callEvent(event);
        player.getOpenInventory().getTopInventory().setItem(53, new ItemStack(Material.IRON_INGOT, 9));
        server.getScheduler().performTicks(10);
        assertTrue(player.saveCalls > 0);
        byte[] saved = player.getPersistentDataContainer().get(new NamespacedKey(plugin, "extra_slots_v1"), PersistentDataType.BYTE_ARRAY);
        assertEquals(new ItemStack(Material.IRON_INGOT, 9), ItemStack.deserializeItemsFromBytes(saved)[26]);
    }

    @Test void legacyExtraSlotsMigrateAfterSaveWithoutLosingVanillaItems() {
        NamespacedKey legacy = NamespacedKey.fromString("enderchestplus:extra_slots_v1");
        NamespacedKey current = new NamespacedKey(plugin, "extra_slots_v1");
        ItemStack[] extra = new ItemStack[27];
        extra[26] = new ItemStack(Material.NETHERITE_INGOT, 5);
        byte[] original = ItemStack.serializeItemsAsBytes(extra);
        player.getPersistentDataContainer().set(legacy, PersistentDataType.BYTE_ARRAY, original);
        player.getEnderChest().setItem(0, new ItemStack(Material.DIAMOND, 10));
        server.dispatchCommand(player, "enderchest");
        Inventory chest = player.getOpenInventory().getTopInventory();
        assertEquals(extra[26], chest.getItem(53));
        assertEquals(new ItemStack(Material.DIAMOND, 10), chest.getItem(0));
        assertArrayEquals(original, player.getPersistentDataContainer().get(legacy, PersistentDataType.BYTE_ARRAY));
        player.closeInventory();
        assertFalse(player.getPersistentDataContainer().has(legacy, PersistentDataType.BYTE_ARRAY));
        assertNotNull(player.getPersistentDataContainer().get(current, PersistentDataType.BYTE_ARRAY));
        server.dispatchCommand(player, "ec");
        assertEquals(extra[26], player.getOpenInventory().getTopInventory().getItem(53));
    }

    @Test void currentDataTakesPrecedenceOverStaleLegacyData() {
        ItemStack[] legacy = new ItemStack[27];
        legacy[0] = new ItemStack(Material.DIAMOND, 1);
        ItemStack[] current = new ItemStack[27];
        current[0] = new ItemStack(Material.EMERALD, 2);
        player.getPersistentDataContainer().set(NamespacedKey.fromString("enderchestplus:extra_slots_v1"), PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(legacy));
        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "extra_slots_v1"), PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(current));
        server.dispatchCommand(player, "enderchest");
        assertEquals(current[0], player.getOpenInventory().getTopInventory().getItem(27));
    }

    @Test void unreadableLegacyDataIsPreservedAndDoesNotOpenAnEmptyChest() {
        NamespacedKey legacy = NamespacedKey.fromString("enderchestplus:extra_slots_v1");
        byte[] corrupt = new byte[] {1, 2, 3};
        player.getPersistentDataContainer().set(legacy, PersistentDataType.BYTE_ARRAY, corrupt);
        server.dispatchCommand(player, "enderchest");
        assertFalse(hasExtendedChest());
        assertArrayEquals(corrupt, player.getPersistentDataContainer().get(legacy, PersistentDataType.BYTE_ARRAY));
        assertFalse(player.getPersistentDataContainer().has(new NamespacedKey(plugin, "extra_slots_v1"), PersistentDataType.BYTE_ARRAY));
    }
    private boolean hasExtendedChest() {
        Inventory top = player.getOpenInventory().getTopInventory();
        return top != null && top.getSize() == 54;
    }
    // MockBukkit does not implement disk writes. Observe the save request while
    // using its real item codec and persistent data container for the roundtrip.
    static class SavingPlayer extends PlayerMock {
        int saveCalls;
        SavingPlayer(ServerMock server, String name) { super(server, name); }
        @Override public void saveData() { saveCalls++; }
    }
}

