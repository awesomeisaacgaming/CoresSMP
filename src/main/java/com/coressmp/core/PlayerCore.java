package com.coressmp.core;

import java.util.UUID;

public class PlayerCore {

    private final UUID playerId;
    private CoreType type;
    private int tier;
    // Ice-specific toggle
    private boolean frostWalkerEnabled = false;

    public PlayerCore(UUID playerId, CoreType type, int tier) {
        this.playerId = playerId;
        this.type = type;
        this.tier = tier;
    }

    public UUID getPlayerId() { return playerId; }
    public CoreType getType() { return type; }
    public void setType(CoreType type) { this.type = type; }

    public int getTier() { return tier; }

    public void setTier(int tier) {
        this.tier = Math.max(0, Math.min(5, tier));
    }

    public void incrementTier() { setTier(tier + 1); }
    public void decrementTier() { setTier(tier - 1); }

    public boolean isFrostWalkerEnabled() { return frostWalkerEnabled; }
    public void toggleFrostWalker() { frostWalkerEnabled = !frostWalkerEnabled; }
}
