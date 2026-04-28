package com.coressmp.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    // key: "uuid:abilityKey"  value: last use time in ms
    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID uuid, String abilityKey) {
        String key = uuid + ":" + abilityKey;
        if (!cooldowns.containsKey(key)) return false;
        long elapsed = System.currentTimeMillis() - cooldowns.get(key);
        return elapsed < getCooldownMillis(abilityKey);
    }

    public long getRemainingSeconds(UUID uuid, String abilityKey) {
        String key = uuid + ":" + abilityKey;
        if (!cooldowns.containsKey(key)) return 0;
        long elapsed = System.currentTimeMillis() - cooldowns.get(key);
        long remaining = getCooldownMillis(abilityKey) - elapsed;
        return Math.max(0, remaining / 1000);
    }

    public void setCooldown(UUID uuid, String abilityKey) {
        cooldowns.put(uuid + ":" + abilityKey, System.currentTimeMillis());
    }

    // Cooldown durations in ms keyed by ability identifier
    private long getCooldownMillis(String abilityKey) {
        return switch (abilityKey) {
            case "fire1" -> 15_000L;
            case "fire2" -> 30_000L;
            case "wind1" -> 0L;
            case "wind2" -> 30_000L;
            case "ender1" -> 30_000L;
            case "ender2" -> 60_000L;
            case "ice1" -> 20_000L;
            case "ice2" -> 45_000L;
            case "ocean1" -> 30_000L;
            case "ocean2" -> 45_000L;
            case "echo1" -> 30_000L;
            case "echo2" -> 60_000L;
            case "wealth1" -> 0L;
            case "wealth2" -> 45_000L;
            default -> 0L;
        };
    }
}
