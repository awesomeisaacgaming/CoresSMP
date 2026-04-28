package com.coressmp.command;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GiveCoreCommand implements CommandExecutor, TabCompleter {

    private final CoresSMP plugin;

    public GiveCoreCommand(CoresSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coressmp.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /givecore <player> <coreName>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
            return true;
        }

        CoreType coreType;
        try {
            coreType = CoreType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown core: " + args[1]);
            sender.sendMessage(ChatColor.YELLOW + "Valid cores: " + Arrays.stream(CoreType.values())
                    .map(Enum::name).collect(Collectors.joining(", ")));
            return true;
        }

        plugin.getCoreManager().setCore(target.getUniqueId(), coreType);
        target.sendMessage(ChatColor.GOLD + "You have been given the " + coreType.name() + " core!");
        sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " the " + coreType.name() + " core.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.stream(CoreType.values())
                    .map(Enum::name)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
