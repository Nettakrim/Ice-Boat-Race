package com.nettakrim.ice_boat;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nettakrim.ice_boat.items.LevitationEffect;

public class PlayerData {
    public PlayerData(Player player) {
        this.player = player;
        this.lastSafeLocation = player.getLocation().add(0,2,0);
    }

    public final Player player;
    public LevitationEffect levitationEffect;
    public Location lastSafeLocation;

    public boolean isLevitating() {
        return levitationEffect != null && !levitationEffect.isCancelled();
    }

    public void cancelLevitation(boolean makeNoise, Entity vehicle) {
        if (!isLevitating()) return;
        if (vehicle == null) levitationEffect.stop(player.getVehicle(), makeNoise);
        else levitationEffect.stop(vehicle, makeNoise);
    }

    public void clear() {
        cancelLevitation(false, null);
    }
}
