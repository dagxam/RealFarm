package me.dagxam.realfarm.season;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/** Optional bridge to RealisticSeasons. Reflection keeps RealFarm loadable without the seasons plugin. */
public final class SeasonIntegration {
    public enum Season { SPRING, SUMMER, FALL, WINTER, UNKNOWN }

    private final JavaPlugin plugin;
    private Method getInstance;
    private Method getSeason;
    private boolean available;

    public SeasonIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        hook();
    }

    private void hook() {
        Plugin seasons = plugin.getServer().getPluginManager().getPlugin("RealisticSeasons");
        if (seasons == null || !seasons.isEnabled()) return;
        try {
            Class<?> api = Class.forName("me.casperge.realisticseasons.api.SeasonsAPI", false, seasons.getClass().getClassLoader());
            getInstance = api.getMethod("getInstance");
            getSeason = api.getMethod("getSeason", World.class);
            available = true;
            plugin.getLogger().info("RealisticSeasons найден: сезонная влажность RealFarm включена.");
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("RealisticSeasons найден, но его API не удалось подключить: " + ex.getMessage());
        }
    }

    public boolean isAvailable() { return available; }

    public Season getSeason(World world) {
        if (!available) return Season.UNKNOWN;
        try {
            Object api = getInstance.invoke(null);
            Object value = getSeason.invoke(api, world);
            if (value == null) return Season.UNKNOWN;
            return Season.valueOf(value.toString().toUpperCase());
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            return Season.UNKNOWN;
        }
    }
}
