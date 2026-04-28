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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OceanCoreListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;
    private final CooldownManager cooldownManager;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public OceanCoreListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    // Tier 3: every 10th hit, push attacker away with water particles
    // Tier 4: extra damage in water
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        PlayerCore core = coreManager.getCore(defender);
        if (core == null || core.getType() != CoreType.OCEAN || core.getTier() == 0) return;

        int tier = core.getTier();

        // Tier 4: extra damage when defender is in water (attacker gets boosted... but logic: extra dmg from ocean player)
        // Reinterpreting: ocean player deals extra damage when in water
    }

    @EventHandler
    public void onOceanPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerCore core = coreManager.getCore(attacker);
        if (core == null || core.getType() != CoreType.OCEAN || core.getTier() == 0) return;

        int tier = core.getTier();

        // Tier 4: extra damage in water
        if (tier >= 4 && attacker.isInWater()) {
            event.setDamage(event.getDamage() + 2.0);
        }

        // Tier 3: every 10th hit pushes attacker away
        if (tier >= 3) {
            int count = hitCounters.getOrDefault(attacker.getUniqueId(), 0) + 1;
            hitCounters.put(attacker.getUniqueId(), count);
            if (count % 10 == 0) {
                if (event.getEntity() instanceof Entity target) {
                    Vector push = target.getLocation().toVector()
                            .subtract(attacker.getLocation().toVector())
                            .normalize().multiply(3.0).setY(0.5);
                    target.setVelocity(push);
                    attacker.getWorld().spawnParticle(Particle.SPLASH, target.getLocation(), 50, 1, 1, 1, 0.1);
                }
            }
        }
    }

    // Ability 1 (Tier 3): Fill 10x10x5 area with water
    public void ability1(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.OCEAN || core.getTier() < 3) {
            player.sendMessage(ChatColor.BLUE + "You need an Ocean core at Tier 3+ for Water Flood.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "ocean1")) {
            player.sendMessage(ChatColor.BLUE + "Water Flood on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "ocean1") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "ocean1");

        Location center = player.getLocation();
        World world = player.getWorld();
        Map<Location, Material> originalBlocks = new HashMap<>();

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y < 5; y++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.AIR) {
                        originalBlocks.put(block.getLocation(), Material.AIR);
                        block.setType(Material.WATER);
                    }
                }
            }
        }

        world.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1f, 1f);
        player.sendMessage(ChatColor.BLUE + "Water Flood!");

        // Remove water after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                    Block b = entry.getKey().getBlock();
                    if (b.getType() == Material.WATER) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }.runTaskLater(plugin, 200L);
    }

    // Ability 2 (Tier 5): Tidal wave
    public void ability2(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.OCEAN || core.getTier() < 5) {
            player.sendMessage(ChatColor.BLUE + "You need an Ocean core at Tier 5 for Tidal Wave.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "ocean2")) {
            player.sendMessage(ChatColor.BLUE + "Tidal Wave on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "ocean2") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "ocean2");

        Location start = player.getLocation();
        Vector direction = player.getLocation().getDirection().setY(0).normalize();

        player.sendMessage(ChatColor.BLUE + "Tidal Wave!");

        // Move a wall of water forward, damaging players in it
        new BukkitRunnable() {
            int step = 0;
            Location waveFront = start.clone();

            @Override
            public void run() {
                if (step >= 15) {
                    cancel();
                    return;
                }
                waveFront.add(direction);

                // Damage entities inside wave
                for (Entity entity : waveFront.getWorld().getNearbyEntities(waveFront, 2, 3, 2)) {
                    if (entity instanceof LivingEntity living && !entity.equals(player)) {
                        living.damage(2.0, player); // 1 heart per second (roughly every 20 ticks)
                    }
                }

                waveFront.getWorld().spawnParticle(Particle.SPLASH, waveFront, 100, 2, 1.5, 2, 0.2);
                waveFront.getWorld().spawnParticle(Particle.FISHING, waveFront, 50, 2, 1.5, 2, 0.1);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
