package com.coressmp.listener;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import com.coressmp.manager.CoreManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PassiveTicker extends BukkitRunnable {

    private final CoresSMP plugin;
    private final CoreManager coreManager;

    private static final NamespacedKey ENDER_NIGHT_KEY = new NamespacedKey("coressmp", "ender_night_heart");
    private static final NamespacedKey ECHO_SNEAK_KEY = new NamespacedKey("coressmp", "echo_swift_sneak");
    private static final NamespacedKey WIND_CHARGE_KEY = new NamespacedKey("coressmp", "wind_charge_item");
    private static final NamespacedKey ENDER_PEARL_KEY = new NamespacedKey("coressmp", "ender_pearl_item");

    public PassiveTicker(CoresSMP plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
    }

    public void start() {
        runTaskTimer(plugin, 20L, 20L); // every second
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerCore core = coreManager.getCore(player);
            if (core == null || core.getTier() == 0) continue;

            applyPassives(player, core);
        }
    }

    private void applyPassives(Player player, PlayerCore core) {
        CoreType type = core.getType();
        int tier = core.getTier();

        switch (type) {
            case FIRE -> applyFirePassives(player, tier);
            case WIND -> applyWindPassives(player, tier);
            case ENDER -> applyEnderPassives(player, tier);
            case ICE -> {} // Ice passives are largely event-based (move events handle speed/regen)
            case OCEAN -> applyOceanPassives(player, tier);
            case ECHO -> applyEchoPassives(player, tier);
            case WEALTH -> applyWealthPassives(player, tier);
        }
    }

    private void applyFirePassives(Player player, int tier) {
        if (tier >= 1) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false));
        }
        if (tier >= 2) {
            // Fire aspect on swords is applied in the damage event handler
            applySwordFireAspect(player);
        }
    }

    private void applySwordFireAspect(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSword(item)) {
                if (!item.containsEnchantment(Enchantment.FIRE_ASPECT)) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                        item.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private void applyWindPassives(Player player, int tier) {
        if (tier >= 3) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false));
        }
        if (tier >= 5) {
            // Give infinite wind charge
            ensurePlayerHasItem(player, Material.WIND_CHARGE, WIND_CHARGE_KEY);
        }
    }

    private void applyEnderPassives(Player player, int tier) {
        if (tier >= 3) {
            // Extra heart at night
            World world = player.getWorld();
            boolean isNight = world.getTime() >= 13000 && world.getTime() <= 23000;
            AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) {
                boolean hasModifier = attr.getModifiers().stream()
                        .anyMatch(m -> m.getKey().equals(ENDER_NIGHT_KEY));

                if (isNight && !hasModifier) {
                    attr.addModifier(new AttributeModifier(ENDER_NIGHT_KEY, 2.0,
                            AttributeModifier.Operation.ADD_NUMBER));
                } else if (!isNight && hasModifier) {
                    attr.getModifiers().stream()
                            .filter(m -> m.getKey().equals(ENDER_NIGHT_KEY))
                            .forEach(attr::removeModifier);
                }
            }
        }
        if (tier >= 5) {
            // Infinite ender pearl
            ensurePlayerHasItem(player, Material.ENDER_PEARL, ENDER_PEARL_KEY);
        }
    }

    private void applyOceanPassives(Player player, int tier) {
        if (tier >= 1) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, true, false));
        }
        if (tier >= 2) {
            applyAquaInfinity(player);
        }
        if (tier >= 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0, true, false));
        }
    }

    private void applyAquaInfinity(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && !helmet.getType().isAir() && !helmet.containsEnchantment(Enchantment.AQUA_AFFINITY)) {
            ItemMeta meta = helmet.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
                helmet.setItemMeta(meta);
                player.getInventory().setHelmet(helmet);
            }
        }
    }

    private void applyEchoPassives(Player player, int tier) {
        if (tier >= 1) {
            applySwiftSneak(player);
        }
        // Tier 2 and 3 are handled in event listeners (sculk suppression, particle amplification)
    }

    private void applySwiftSneak(Player player) {
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null && !leggings.getType().isAir()) {
            if (!leggings.containsEnchantment(Enchantment.SWIFT_SNEAK)) {
                ItemMeta meta = leggings.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(Enchantment.SWIFT_SNEAK, 3, true);
                    leggings.setItemMeta(meta);
                    player.getInventory().setLeggings(leggings);
                }
            }
        }
    }

    private void applyWealthPassives(Player player, int tier) {
        if (tier >= 1) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 60, 0, true, false));
        }
    }

    private void ensurePlayerHasItem(Player player, Material material, NamespacedKey key) {
        boolean hasIt = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && item.hasItemMeta()
                    && item.getItemMeta().getPersistentDataContainer().has(key,
                    org.bukkit.persistence.PersistentDataType.STRING)) {
                hasIt = true;
                break;
            }
        }
        if (!hasIt) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(key,
                        org.bukkit.persistence.PersistentDataType.STRING, "core_passive");
                meta.displayName(net.kyori.adventure.text.Component.text(
                        material == Material.WIND_CHARGE ? "Infinite Wind Charge" : "Infinite Ender Pearl",
                        net.kyori.adventure.text.format.NamedTextColor.AQUA
                ));
                item.setItemMeta(meta);
            }
            player.getInventory().addItem(item);
        }
    }

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("_SWORD");
    }
}
