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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class WealthCoreListener implements Listener {

    private final CoresSMP plugin;
    private final CoreManager coreManager;
    private final CooldownManager cooldownManager;
    private final Map<UUID, Integer> blockBreakCounters = new HashMap<>();

    private static final Set<Material> ORE_MATERIALS = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    public WealthCoreListener(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.cooldownManager = plugin.getCooldownManager();
        startOreHighlightTicker();
    }

    // Tier 2: every 10th block broken gives 1 diamond
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WEALTH || core.getTier() < 2) return;

        int count = blockBreakCounters.getOrDefault(player.getUniqueId(), 0) + 1;
        blockBreakCounters.put(player.getUniqueId(), count);

        if (count % 10 == 0) {
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, 1));
            player.sendMessage(ChatColor.GREEN + "+1 Diamond from Wealth!");
        }
    }

    // Tier 3: every mob kill gives 5-10 emeralds
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WEALTH || core.getTier() < 3) return;

        if (event.getEntity() instanceof Monster || event.getEntity() instanceof Animals) {
            int amount = 5 + new Random().nextInt(6); // 5-10
            player.getInventory().addItem(new ItemStack(Material.EMERALD, amount));
            player.sendMessage(ChatColor.GREEN + "+" + amount + " Emeralds from Wealth!");
        }
    }

    // Tier 5: ore highlight in 10 block radius
    private void startOreHighlightTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerCore core = coreManager.getCore(player);
                    if (core == null || core.getType() != CoreType.WEALTH || core.getTier() < 5) continue;

                    Location center = player.getLocation();
                    for (int x = -10; x <= 10; x++) {
                        for (int y = -10; y <= 10; y++) {
                            for (int z = -10; z <= 10; z++) {
                                Block b = center.clone().add(x, y, z).getBlock();
                                if (ORE_MATERIALS.contains(b.getType())) {
                                    Location loc = b.getLocation().add(0.5, 0.5, 0.5);
                                    player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 3, 0.2, 0.2, 0.2, 0);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // every 2 seconds
    }

    // Ability 1 (Tier 3): Money Trail - Speed 3 for 5 seconds
    public void ability1(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WEALTH || core.getTier() < 3) {
            player.sendMessage(ChatColor.GREEN + "You need a Wealth core at Tier 3+ for Money Trail.");
            return;
        }
        // No cooldown per spec but add a small one to prevent spam
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2)); // speed 3 = amplifier 2

        // Green particle trail
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100 || !player.isOnline()) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 5, 0.2, 0.2, 0.2, 0);
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        player.sendMessage(ChatColor.GREEN + "Money Trail!");
    }

    // Ability 2 (Tier 5): Giant emerald drops from sky
    public void ability2(Player player) {
        PlayerCore core = coreManager.getCore(player);
        if (core == null || core.getType() != CoreType.WEALTH || core.getTier() < 5) {
            player.sendMessage(ChatColor.GREEN + "You need a Wealth core at Tier 5 for Emerald Drop.");
            return;
        }
        if (cooldownManager.isOnCooldown(player.getUniqueId(), "wealth2")) {
            player.sendMessage(ChatColor.GREEN + "Emerald Drop on cooldown! " + cooldownManager.getRemainingSeconds(player.getUniqueId(), "wealth2") + "s remaining.");
            return;
        }
        cooldownManager.setCooldown(player.getUniqueId(), "wealth2");

        Location target = player.getTargetBlock(null, 30).getLocation().add(0.5, 0, 0.5);
        Location spawnLoc = target.clone().add(0, 20, 0);

        // Drop a falling emerald block
        FallingBlock emerald = player.getWorld().spawnFallingBlock(spawnLoc, Material.EMERALD_BLOCK.createBlockData());
        emerald.setDropItem(false);
        emerald.setHurtEntities(true);
        emerald.setDamagePerBlock(1.0f);

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, spawnLoc, 30, 1, 1, 1, 0.1);
        player.sendMessage(ChatColor.GREEN + "Emerald Drop!");

        // After landing, damage nearby entities
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!emerald.isValid()) {
                    // Deal 2 hearts to nearby entities at last location
                    for (Entity entity : target.getWorld().getNearbyEntities(target, 2, 2, 2)) {
                        if (entity instanceof LivingEntity living && !entity.equals(player)) {
                            living.damage(4.0, player);
                        }
                    }
                    target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.add(0, 1, 0), 50, 1, 1, 1, 0.1);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }
}
