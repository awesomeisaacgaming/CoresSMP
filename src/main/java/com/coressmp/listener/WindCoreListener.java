package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CooldownManager;
import com.coressmp.manager.CoreManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class WindCoreListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;
    private final CooldownManager cooldownManager;

    public WindCoreListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    // Tier 1: no fall damage
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WIND || core.getTier() < 1) return;
        event.setCancelled(true);
    }

    // Tier 2: walk through cobwebs (handled by clearing block data tick - we cancel movement slow)
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WIND || core.getTier() < 2) return;

        Block block = player.getLocation().getBlock();
        if (block.getType() == Material.COBWEB) {
            player.setVelocity(player.getVelocity().multiply(1.5));
        }
    }

    // Ability 1 (Tier 3): Leap 25 blocks
    public void ability1(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WIND || core.getTier() < 3) {
            player.sendMessage(ChatColor.GRAY + "You need a Wind core at Tier 3+ for Leap.");
            return;
        }
        // No cooldown for leap per spec
        Vector dir = player.getLocation().getDirection().normalize().multiply(2.5).setY(1.0);
        player.setVelocity(dir);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1f, 1f);
        player.sendMessage(ChatColor.AQUA + "Leap!");
    }

    // Ability 2 (Tier 5): Tornado ride
    public void ability2(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WIND || core.getTier() < 5) {
            player.sendMessage(ChatColor.GRAY + "You need a Wind core at Tier 5 for Tornado.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "wind2")) {
            player.sendMessage(ChatColor.GRAY + "Tornado on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "wind2") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "wind2");

        player.sendMessage(ChatColor.AQUA + "Tornado summoned!");

        // Ride a temporary wind charge entity (simulate by propelling forward and damaging nearby)
        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 200; // 10 seconds

            @Override
            public void run() {
                if (ticks >= duration || !player.isOnline()) {
                    cancel();
                    return;
                }

                if (ticks % 2 == 0) {
                    Vector dir = player.getLocation().getDirection().normalize().multiply(0.6).setY(0.05);
                    player.setVelocity(dir);
                }

                // Particle spiral
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);

                // Damage nearby entities every 10 ticks
                if (ticks % 10 == 0) {
                    for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                        if (entity instanceof LivingEntity living && !entity.equals(player)) {
                            living.damage(3.0, player); // 1.5 hearts
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
