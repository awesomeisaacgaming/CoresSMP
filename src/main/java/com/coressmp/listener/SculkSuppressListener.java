package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CoreManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockReceiveGameEvent;

public class SculkSuppressListener implements Listener {

    private final CoreManager coreManager;

    public SculkSuppressListener(CoresSMP plugin) {
        this.coreManager = plugin.getCoreManager();
    }

    // Tier 2: Echo players don't activate sculk sensors/shriekers
    @EventHandler
    public void onSculkSensor(BlockReceiveGameEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ECHO || core.getTier() < 2) return;

        Block block = event.getBlock();
        if (block.getType() == Material.SCULK_SENSOR
                || block.getType() == Material.CALIBRATED_SCULK_SENSOR
                || block.getType() == Material.SCULK_SHRIEKER) {
            event.setCancelled(true);
        }
    }
}
