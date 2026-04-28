package com.coressmp.manager;

import com.coressmp.CoresSMP;
import com.coressmp.core.CoreType;
import com.coressmp.core.PlayerCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CoreManager {

    private final CoresSMP plugin;
    private final Map<UUID, PlayerCore> cores = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public CoreManager(CoresSMP plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                String typeName = dataConfig.getString("players." + key + ".type");
                int tier = dataConfig.getInt("players." + key + ".tier", 1);
                try {
                    CoreType type = CoreType.valueOf(typeName);
                    cores.put(uuid, new PlayerCore(uuid, type, tier));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerCore> entry : cores.entrySet()) {
            String key = "players." + entry.getKey().toString();
            dataConfig.set(key + ".type", entry.getValue().getType().name());
            dataConfig.set(key + ".tier", entry.getValue().getTier());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public void savePlayer(UUID uuid) {
        PlayerCore core = cores.get(uuid);
        if (core == null) return;
        String key = "players." + uuid;
        dataConfig.set(key + ".type", core.getType().name());
        dataConfig.set(key + ".tier", core.getTier());
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public PlayerCore getCore(UUID uuid) {
        return cores.get(uuid);
    }

    public PlayerCore getCore(Player player) {
        return cores.get(player.getUniqueId());
    }

    public boolean hasCore(UUID uuid) {
        return cores.containsKey(uuid);
    }

    public void assignRandomCore(Player player) {
        CoreType[] types = CoreType.values();
        CoreType chosen = types[new Random().nextInt(types.length)];
        PlayerCore core = new PlayerCore(player.getUniqueId(), chosen, 1);
        cores.put(player.getUniqueId(), core);
        savePlayer(player.getUniqueId());
    }

    public void setCore(UUID uuid, CoreType type) {
        PlayerCore existing = cores.get(uuid);
        if (existing != null) {
            existing.setType(type);
            existing.setTier(1);
        } else {
            cores.put(uuid, new PlayerCore(uuid, type, 1));
        }
        savePlayer(uuid);
    }

    public void onKill(UUID uuid) {
        PlayerCore core = cores.get(uuid);
        if (core != null) {
            core.incrementTier();
            savePlayer(uuid);
        }
    }

    public void onDeath(UUID uuid) {
        PlayerCore core = cores.get(uuid);
        if (core != null) {
            core.decrementTier();
            savePlayer(uuid);
        }
    }
}
