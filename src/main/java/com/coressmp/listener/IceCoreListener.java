package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CooldownManager;
import com.coressmp.manager.CoreManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IceCoreListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;
    private final CooldownManager cooldownManager;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    public IceCoreListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    // Tier 2: can't fall through powder snow - prevent entering powder snow blocks
    // Powder snow sinking is handled by EntityDamage/move - we just negate it
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ICE) return;

        int tier = core.getTier();
        if (tier < 2) return;

        if (player.getLocation().getBlock().getType() == Material.POWDER_SNOW) {
            event.setCancelled(true);
        }

        // Tier 4: speed 1 on ice
        if (tier >= 4) {
            Material below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
            if (below == Material.ICE || below == Material.PACKED_ICE || below == Material.BLUE_ICE || below == Material.FROSTED_ICE) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false));
            }
        }

        // Tier 5: regen on snow/ice
        if (tier >= 5) {
            Material standing = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
            if (standing == Material.SNOW || standing == Material.SNOW_BLOCK
                    || standing == Material.ICE || standing == Material.PACKED_ICE
                    || standing == Material.BLUE_ICE || standing == Material.FROSTED_ICE
                    || standing == Material.POWDER_SNOW) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, true, false));
            }
        }
    }

    // Tier 3: every 10th hit gives slowness 1 for 3 sec
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerCore core = coreManager.getCore(attacker);
        if (core == null || core.getType() != CoreType.ICE || core.getTier() < 3) return;

        int count = hitCounters.getOrDefault(attacker.getUniqueId(), 0) + 1;
        hitCounters.put(attacker.getUniqueId(), count);

        if (count % 10 == 0) {
            if (event.getEntity() instanceof LivingEntity target) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
            }
        }
    }

    // Ability 1 (Tier 3): Freeze all nearby entities 3-5 sec
    public void ability1(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ICE || core.getTier() < 3) {
            player.sendMessage(ChatColor.AQUA + "You need an Ice core at Tier 3+ for Freeze.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "ice1")) {
            player.sendMessage(ChatColor.AQUA + "Freeze on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "ice1") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "ice1");

        int freezeTicks = (int) ((3 + Math.random() * 2) * 20); // 3-5 seconds

        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, freezeTicks, 6));
                living.setFreezeTicks(freezeTicks);
            }
        }
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 100, 5, 5, 5, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 2f, 0.5f);
        player.sendMessage(ChatColor.AQUA + "Freeze!");
    }

    // Ability 2 (Tier 5): Burst 3 ice blocks
    public void ability2(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.ICE || core.getTier() < 5) {
            player.sendMessage(ChatColor.AQUA + "You need an Ice core at Tier 5 for Ice Burst.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "ice2")) {
            player.sendMessage(ChatColor.AQUA + "Ice Burst on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "ice2") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "ice2");

        // Throw 3 ice blocks in a burst (not simultaneously) - 5 ticks apart each
        for (int i = 0; i < 3; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Snowball iceProjectile = player.launchProjectile(Snowball.class);
                    iceProjectile.setVelocity(player.getLocation().getDirection().normalize().multiply(2.5));
                    // Tag projectile for ice damage
                    iceProjectile.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(plugin, "ice_burst"),
                            org.bukkit.persistence.PersistentDataType.STRING,
                            player.getUniqueId().toString()
                    );
                }
            }.runTaskLater(plugin, index * 5L);
        }
        player.sendMessage(ChatColor.AQUA + "Ice Burst!");
    }

    // Handle ice burst projectile hit
    @EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        String ownerUUID = snowball.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "ice_burst"),
                org.bukkit.persistence.PersistentDataType.STRING
        );
        if (ownerUUID == null) return;
        if (event.getHitEntity() instanceof LivingEntity target) {
            target.damage(2.0); // 1 heart
            target.setFreezeTicks(60);
        }
    }
}
