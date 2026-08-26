package me.dagxam.realfarm.farm;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/** Runtime bridge to RealisticSeasons 11.12.1. Uses its public SeasonsAPI when the plugin is installed. */
public final class SeasonBridge {
    public enum FarmSeason { SPRING, SUMMER, FALL, WINTER, UNKNOWN }

    public FarmSeason getSeason(World world) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("RealisticSeasons");
        if (plugin == null || !plugin.isEnabled()) return FarmSeason.UNKNOWN;
        try {
            Class<?> api = Class.forName("me.casperge.realisticseasons.api.SeasonsAPI");
            Object instance = api.getMethod("getInstance").invoke(null);
            Object season = api.getMethod("getSeason", World.class).invoke(instance, world);
            return switch (((Enum<?>) season).name()) {
                case "SPRING" -> FarmSeason.SPRING;
                case "SUMMER" -> FarmSeason.SUMMER;
                case "FALL" -> FarmSeason.FALL;
                case "WINTER" -> FarmSeason.WINTER;
                default -> FarmSeason.UNKNOWN;
            };
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return FarmSeason.UNKNOWN;
        }
    }

    public boolean isRaining(World world) {
        return world.hasStorm() && !world.isThundering() || world.hasStorm();
    }
}
