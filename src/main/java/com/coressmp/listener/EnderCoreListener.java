package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CooldownManager;
import com.coressmp.manager.CoreManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderCoreListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;
    private final CooldownManager cooldownManager;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public EnderCoreListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    // Tier 1: every 10th hit, give attacker slowness + darkness
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerCore core = coreManager.getCore(attacker);
        if (core == null || core.getType() != CoreType.ENDER || core.getTier() < 1) return;

        int count = hitCounters.getOrDefault(attacker.getUniqueId(), 0) + 1;
        hitCounters.put(attacker.getUniqueId(), count);

        if (count % 10 == 0) {
            if (event.getEntity() instanceof LivingEntity target) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
            }
        }
    }

    // Tier 2: can't die to void
    @EventHandler
    public void onEntityDamageGeneral(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ENDER || core.getTier() < 2) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            // Teleport above void
            if (player.getLocation().getY() < -60) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        }
    }

    // Tier 3: extra heart at night (via attribute modifier tick - managed in PassiveTicker)
    // Handled in PassiveTicker

    // Tier 4: mobs don't aggro
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ENDER || core.getTier() < 4) return;
        if (!(event.getEntity() instanceof Monster)) return;
        event.setCancelled(true);
    }

    // Ability 1 (Tier 3): Black hole - deals 2 hearts to nearby entities
    public void ability1(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ENDER || core.getTier() < 3) {
            player.sendMessage(ChatColor.DARK_PURPLE + "You need an Ender core at Tier 3+ for Black Hole.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "ender1")) {
            player.sendMessage(ChatColor.DARK_PURPLE + "Black Hole on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "ender1") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "ender1");

        Location loc = player.getTargetBlock(null, 20).getLocation().add(0.5, 1, 0.5);
        World world = player.getWorld();
        world.spawnParticle(Particle.PORTAL, loc, 200, 1, 1, 1, 0.5);
        world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 0.5f);

        // Pull entities toward black hole and deal damage
        for (Entity entity : world.getNearbyEntities(loc, 5, 5, 5)) {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                // Pull toward loc
                Vector pull = loc.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.5);
                entity.setVelocity(pull);
                living.damage(4.0, player); // 2 hearts
            }
        }

        player.sendMessage(ChatColor.DARK_PURPLE + "Black Hole!");
    }

    // Ability 2 (Tier 5): Summon ender dragon to ride
    public void ability2(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ENDER || core.getTier() < 5) {
            player.sendMessage(ChatColor.DARK_PURPLE + "You need an Ender core at Tier 5 for Dragon Ride.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "ender2")) {
            player.sendMessage(ChatColor.DARK_PURPLE + "Dragon Ride on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "ender2") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "ender2");

        EnderDragon dragon = (EnderDragon) player.getWorld().spawnEntity(player.getLocation(), EntityType.ENDER_DRAGON);
        dragon.setPhase(EnderDragon.Phase.HOVER);
        dragon.setMaxHealth(200);
        dragon.setHealth(200);
        dragon.addPassenger(player);
        player.sendMessage(ChatColor.DARK_PURPLE + "Ender Dragon summoned!");

        // Dragon damages players it touches
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 200 || !dragon.isValid() || !player.isOnline()) {
                    if (dragon.isValid()) dragon.remove();
                    cancel();
                    return;
                }
                // Damage nearby players
                for (Entity entity : dragon.getNearbyEntities(3, 3, 3)) {
                    if (entity instanceof Player target && !target.equals(player)) {
                        target.damage(2.0, player); // 1 heart
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Remove after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dragon.isValid()) dragon.remove();
                player.sendMessage(ChatColor.DARK_PURPLE + "Your dragon fades away.");
            }
        }.runTaskLater(plugin, 200L);
    }
}
