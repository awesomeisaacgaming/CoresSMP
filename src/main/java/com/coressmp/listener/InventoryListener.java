package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CoreManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InventoryListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;

    // There is no physical "core item" per the design — the core is stored as data.
    // However, to visually represent the core, we mark a special item in slot 8 (hotbar).
    // This listener prevents moving/dropping that item and prevents placing anything in chests
    // that has the core tag.
    // Since the spec says cores can only be in player inventory (not dropped, not in chests),
    // and are data-based, we implement an info item that the player can see but cannot lose.

    public InventoryListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
    }

    // We use a special named item approach: if someone tries to move a core item, cancel it.
    // The actual core item is managed and placed back in slot 8 automatically.

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        // Prevent moving the core item out of player inventory into any external inventory
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING
                && event.getView().getTopInventory().getType() != InventoryType.PLAYER) {
            // External inventory is open (chest, etc.)
            if (isCoreItem(clicked) || isCoreItem(cursor)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place your core in external inventories!");
            }
            // Cancel shift-click of core item into chest
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isCoreItem(clicked)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot place your core in external inventories!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isCoreItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot drop your core!");
        }
    }

    private boolean isCoreItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "core_item"),
                PersistentDataType.STRING
        );
    }
}
