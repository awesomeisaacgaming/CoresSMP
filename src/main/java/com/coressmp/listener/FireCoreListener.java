package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CooldownManager;
import com.coressmp.manager.CoreManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class FireCoreListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;
    private final CooldownManager cooldownManager;

    public FireCoreListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.cooldownManager = plugin.getCooldownManager();
    }

    // Tier 1: Permanent fire resistance via potion tick (handled in tick scheduler in PlayerJoinListener)
    // Tier 2: Fire Aspect on every sword
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        PlayerCore core = coreManager.getCore(attacker);
        if (core == null || core.getType() != CoreType.FIRE || core.getTier() == 0) return;

        int tier = core.getTier();

        // Tier 2: fire aspect on swords
        if (tier >= 2) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (isSword(held) && !held.containsEnchantment(Enchantment.FIRE_ASPECT)) {
                if (event.getEntity() instanceof LivingEntity target) {
                    target.setFireTicks(80);
                }
            }
        }

        // Tier 3: extra damage when attacker is on fire
        if (tier >= 3 && attacker.getFireTicks() > 0) {
            event.setDamage(event.getDamage() + 1.5);
        }

        // Tier 5: when hit, set the attacker on fire (handled in defender side below)
    }

    // Tier 5: when defender is hit, light up the attacker
    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        PlayerCore core = coreManager.getCore(defender);
        if (core == null || core.getType() != CoreType.FIRE || core.getTier() < 5) return;

        if (event.getDamager() instanceof LivingEntity attacker) {
            attacker.setFireTicks(100);
        }
    }

    // Tier 4: lava walker - convert lava to magma temporarily
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.FIRE || core.getTier() < 4) return;

        Block below = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        if (below.getType() == Material.LAVA) {
            below.setType(Material.MAGMA_BLOCK);
            Block finalBelow = below;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (finalBelow.getType() == Material.MAGMA_BLOCK) {
                        finalBelow.setType(Material.LAVA);
                    }
                }
            }.runTaskLater(plugin, 60L);
        }
    }

    // Ability 1 (Tier 3): Magma Lunge
    public void ability1(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.FIRE || core.getTier() < 3) {
            player.sendMessage(ChatColor.RED + "You need a Fire core at Tier 3+ for Magma Lunge.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "fire1")) {
            player.sendMessage(ChatColor.RED + "Magma Lunge on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "fire1") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "fire1");

        // Lunge player forward
        org.bukkit.util.Vector dir = player.getLocation().getDirection().normalize().multiply(1.5).setY(0.3);
        player.setVelocity(dir);
        player.setFireTicks(40);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

        // Damage entities in path after short delay
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity nearby : player.getNearbyEntities(2, 2, 2)) {
                    if (nearby instanceof LivingEntity target && !target.equals(player)) {
                        double dmg = 2.0 + new Random().nextDouble() * 2.0; // 1-2 hearts = 2-4 hp
                        target.damage(dmg, player);
                        target.setFireTicks(60);
                    }
                }
            }
        }.runTaskLater(plugin, 6L);

        player.sendMessage(ChatColor.GOLD + "Magma Lunge!");
    }

    // Ability 2 (Tier 5): Fireball explosion
    public void ability2(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.FIRE || core.getTier() < 5) {
            player.sendMessage(ChatColor.RED + "You need a Fire core at Tier 5 for Fireball.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "fire2")) {
            player.sendMessage(ChatColor.RED + "Fireball on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "fire2") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "fire2");

        Location target = player.getTargetBlock(null, 30).getLocation().add(0.5, 0.5, 0.5);
        World world = player.getWorld();
        world.spawnParticle(Particle.EXPLOSION, target, 5, 1, 1, 1, 0);
        world.playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);

        // Deal 3 hearts (6 hp) in 5x5 radius
        for (Entity entity : world.getNearbyEntities(target, 2.5, 2.5, 2.5)) {
            if (entity instanceof LivingEntity living && !entity.equals(player)) {
                living.damage(6.0, player);
                living.setFireTicks(100);
            }
        }

        // Spawn fire blocks in radius (no block break)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block b = target.clone().add(x, 0, z).getBlock();
                if (b.getType() == Material.AIR) {
                    b.setType(Material.FIRE);
                    Block fb = b;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (fb.getType() == Material.FIRE) fb.setType(Material.AIR);
                        }
                    }.runTaskLater(plugin, 60L);
                }
            }
        }

        player.sendMessage(ChatColor.GOLD + "Fireball launched!");
    }

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_SWORD");
    }
}
