package com.nettakrim.ice_boat;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;

public class IceBoatBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull PluginProviderContext context) {
        
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new IceBoat();
    }
    
}