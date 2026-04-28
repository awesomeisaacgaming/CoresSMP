package com.coressmp.command;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class IceToggleCommand implements CommandExecutor {

    private final CoresSMP plugin;

    public IceToggleCommand(CoresSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        PlayerCore core = plugin.getCoreManager().getCore(player);
        if (core == null || core.getType() != CoreType.ICE || core.getTier() < 1) {
            player.sendMessage(ChatColor.AQUA + "You need an Ice core at Tier 1+ to use frost walker.");
            return true;
        }

        core.toggleFrostWalker();
        boolean enabled = core.isFrostWalkerEnabled();

        applyFrostWalker(player, enabled);

        player.sendMessage(ChatColor.AQUA + "Frost Walker: " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        return true;
    }

    private void applyFrostWalker(Player player, boolean enable) {
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType().isAir()) return;

        ItemMeta meta = boots.getItemMeta();
        if (meta == null) return;

        if (enable) {
            meta.addEnchant(Enchantment.FROST_WALKER, 2, true);
        } else {
            meta.removeEnchant(Enchantment.FROST_WALKER);
        }

        boots.setItemMeta(meta);
        player.getInventory().setBoots(boots);
    }
}
