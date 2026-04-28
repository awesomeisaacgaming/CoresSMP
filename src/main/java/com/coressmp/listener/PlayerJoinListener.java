package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.manager.CoreManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;

    public PlayerJoinListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!coreManager.hasCore(player.getUniqueId())) {
            coreManager.assignRandomCore(player);
            var core = coreManager.getCore(player);
            player.sendMessage(ChatColor.GOLD + "Welcome! You have been assigned the "
                    + ChatColor.YELLOW + core.getType().name()
                    + ChatColor.GOLD + " core at Tier 1!");
        } else {
            var core = coreManager.getCore(player);
            player.sendMessage(ChatColor.GOLD + "Your core: "
                    + ChatColor.YELLOW + core.getType().name()
                    + ChatColor.GOLD + " | Tier: " + ChatColor.YELLOW + core.getTier());
        }
    }
}
