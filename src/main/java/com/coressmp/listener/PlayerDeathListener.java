package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CoreManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;

    public PlayerDeathListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        coreManager.onDeath(player.getUniqueId());

        PlayerCore core = coreManager.getCore(player);
        if (core != null) {
            player.sendMessage(ChatColor.RED + "You lost a tier! Core: "
                    + core.getType().name() + " | New Tier: " + core.getTier());
        }

        // Check if killer is also a player -> give them a tier
        if (player.getKiller() instanceof Player killer) {
            coreManager.onKill(killer.getUniqueId());
            PlayerCore killerCore = coreManager.getCore(killer);
            if (killerCore != null) {
                killer.sendMessage(ChatColor.GREEN + "Kill reward! Core: "
                        + killerCore.getType().name() + " | New Tier: " + killerCore.getTier());
            }
        }
    }
}
