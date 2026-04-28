package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CoreManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

public class WealthTotemListener implements Listener {

    private final CoreManager coreManager;

    public WealthTotemListener(CoresSMP plugin) {
        this.coreManager = plugin.getCoreManager();
    }

    // Tier 4: When a Wealth player kills someone, ignore their totem
    // We track who the Wealth player's last hit was on and cancel the resurrection if applicable
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        // Check if the killer of this player is a Wealth Tier 4+ player
        Player killer = victim.getKiller();
        if (killer == null) return;

        PlayerCore killerCore = coreManager.getCore(killer);
        if (killerCore == null || killerCore.getType() != CoreType.WEALTH || killerCore.getTier() < 4) return;

        // Cancel totem use
        event.setCancelled(true);
    }
}
