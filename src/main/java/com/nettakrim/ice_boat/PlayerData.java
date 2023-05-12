package com.nettakrim.ice_boat;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nettakrim.ice_boat.items.LevitationEffect;

public class PlayerData {
    public PlayerData(Player player) {
        this.player = player;
    }

    public Player player;
    public LevitationEffect levitationEffect;
    public Location lastSafeLocation;

    public boolean isLevitating() {
        return levitationEffect != null && !levitationEffect.isCancelled();
    }

    public void cancelLevitation(boolean silently) {
        if (!isLevitating()) return;
        if (silently) {
            levitationEffect.cancelSilently();
        } else {
            levitationEffect.cancel();
        }
    }
}
