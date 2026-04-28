package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CooldownManager;
import com.coressmp.manager.CoreManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EchoCoreListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;
    private final CooldownManager cooldownManager;
    private final Map<UUID, Integer> blockBreakCounters = new HashMap<>();

    public EchoCoreListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.cooldownManager = plugin.getCooldownManager();
        startGlowingTicker();
    }

    // Tier 4: every 10th block broken gives 5 xp levels
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ECHO || core.getTier() < 4) return;

        int count = blockBreakCounters.getOrDefault(player.getUniqueId(), 0) + 1;
        blockBreakCounters.put(player.getUniqueId(), count);
        if (count % 10 == 0) {
            player.giveExpLevels(5);
            player.sendMessage(ChatColor.DARK_GREEN + "+5 XP levels from Echo resonance!");
        }
    }

    // Tier 5: every 5 minutes, all players within 30-50 blocks get glowing for 30s
    private void startGlowingTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerCore core = coreManager.getCore(player);
                    if (core == null || core.getType() != CoreType.ECHO || core.getTier() < 5) continue;

                    double radius = 30 + Math.random() * 20; // 30-50
                    for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                        if (entity instanceof Player target) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 600, 0));
                            target.sendMessage(ChatColor.DARK_GREEN + "An Echo core revealed you!");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // every 5 minutes
    }

    // Ability 1 (Tier 3): +2 extra hearts for 30 sec
    public void ability1(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ECHO || core.getTier() < 3) {
            player.sendMessage(ChatColor.DARK_GREEN + "You need an Echo core at Tier 3+ for Echo Shield.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "echo1")) {
            player.sendMessage(ChatColor.DARK_GREEN + "Echo Shield on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "echo1") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "echo1");

        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            AttributeModifier mod = new AttributeModifier(
                    new NamespacedKey(plugin, "echo_hearts"),
                    4.0, // 2 hearts = 4 hp
                    AttributeModifier.Operation.ADD_NUMBER
            );
            attr.addModifier(mod);
            player.setHealth(Math.min(player.getHealth() + 4.0, attr.getValue()));

            // Remove after 30 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    attr.getModifiers().stream()
                            .filter(m -> m.getKey().getKey().equals("echo_hearts"))
                            .forEach(attr::removeModifier);
                }
            }.runTaskLater(plugin, 600L);
        }

        player.getWorld().spawnParticle(Particle.SCULK_SOUL, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.05);
        player.sendMessage(ChatColor.DARK_GREEN + "Echo Shield activated! +2 hearts for 30 seconds.");
    }

    // Ability 2 (Tier 5): Warden sonic attack
    public void ability2(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ECHO || core.getTier() < 5) {
            player.sendMessage(ChatColor.DARK_GREEN + "You need an Echo core at Tier 5 for Sonic Boom.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "echo2")) {
            player.sendMessage(ChatColor.DARK_GREEN + "Sonic Boom on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "echo2") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "echo2");

        // Simulate warden ranged sonic attack as a projectile beam
        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection().normalize();

        player.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
        player.sendMessage(ChatColor.DARK_GREEN + "Sonic Boom!");

        // Ray-cast forward, damaging entities in a 20-block line
        for (double d = 1; d <= 20; d += 0.5) {
            Location point = origin.clone().add(dir.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.SCULK_SOUL, point, 3, 0.1, 0.1, 0.1, 0.02);
            for (Entity entity : point.getWorld().getNearbyEntities(point, 1, 1, 1)) {
                if (entity instanceof LivingEntity living && !entity.equals(player)) {
                    living.damage(6.0, player); // 3 hearts
                    break;
                }
            }
        }
    }
}
