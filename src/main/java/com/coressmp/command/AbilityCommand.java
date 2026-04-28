package com.coressmp.command;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AbilityCommand implements CommandExecutor {

    private final CoresSMP plugin;
    private final int slot;

    public AbilityCommand(CoresSMP plugin, int slot) {
        this.plugin = plugin;
        this.slot = slot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        PlayerCore core = plugin.getCoreManager().getCore(player);
        if (core == null || core.getTier() == 0) {
            player.sendMessage(ChatColor.RED + "Your core is at tier 0 and does nothing.");
            return true;
        }

        CoreType type = core.getType();

        if (slot == 1) {
            switch (type) {
                case FIRE -> plugin.getFireListener().ability1(player);
                case WIND -> plugin.getWindListener().ability1(player);
                case ENDER -> plugin.getEnderListener().ability1(player);
                case ICE -> plugin.getIceListener().ability1(player);
                case OCEAN -> plugin.getOceanListener().ability1(player);
                case ECHO -> plugin.getEchoListener().ability1(player);
                case WEALTH -> plugin.getWealthListener().ability1(player);
            }
        } else {
            switch (type) {
                case FIRE -> plugin.getFireListener().ability2(player);
                case WIND -> plugin.getWindListener().ability2(player);
                case ENDER -> plugin.getEnderListener().ability2(player);
                case ICE -> plugin.getIceListener().ability2(player);
                case OCEAN -> plugin.getOceanListener().ability2(player);
                case ECHO -> plugin.getEchoListener().ability2(player);
                case WEALTH -> plugin.getWealthListener().ability2(player);
            }
        }

        return true;
    }
}
