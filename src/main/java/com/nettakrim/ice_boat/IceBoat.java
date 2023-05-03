package com.nettakrim.ice_boat;

import org.bukkit.plugin.java.JavaPlugin;

import com.nettakrim.ice_boat.commands.TestCommand;

public class IceBoat extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BoatListener(), this);

        this.getCommand("test").setExecutor(new TestCommand());
    }
}
