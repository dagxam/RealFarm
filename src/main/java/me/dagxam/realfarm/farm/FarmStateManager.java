package me.dagxam.realfarm.farm;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class FarmStateManager {
    private static final long DAY_TICKS = 24_000L;

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Long> nextWaterUse = new HashMap<>();
    private final Map<String, Long> fertilizerEnds = new HashMap<>();

    public FarmStateManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "farm-state.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public boolean isFertilizerActive(FarmStructure farm) {
        Long ends = fertilizerEnds.get(farm.id());
        return ends != null && ends > farm.world().getFullTime();
    }

    public void refresh(FarmStructure farm) {
        refreshWater(farm);
        refreshComposter(farm);
    }

    public void tick() {
        for (World world : Bukkit.getWorlds()) {
            long now = world.getFullTime();

            for (String key : nextWaterUse.keySet().toArray(String[]::new)) {
                if (!key.startsWith(world.getUID().toString() + ":")) continue;
                long due = nextWaterUse.get(key);
                if (due <= now) {
                    Block cauldron = findBlock(world, key);
                    if (cauldron != null && cauldron.getType() == Material.WATER_CAULDRON) {
                        cauldron.setType(Material.CAULDRON);
                    }
                    nextWaterUse.remove(key);
                }
            }

            for (String key : fertilizerEnds.keySet().toArray(String[]::new)) {
                if (!key.startsWith(world.getUID().toString() + ":")) continue;
                long due = fertilizerEnds.get(key);
                if (due <= now) {
                    Block cauldron = findBlock(world, key);
                    if (cauldron != null) {
                        Block composter = findAdjacentComposter(cauldron);
                        if (composter != null && composter.getBlockData() instanceof Levelled levelled) {
                            levelled.setLevel(0);
                            composter.setBlockData(levelled);
                        }
                    }
                    fertilizerEnds.remove(key);
                }
            }
        }
        save();
    }

    private void refreshWater(FarmStructure farm) {
        String id = farm.id();
        if (!farm.isWatered()) {
            nextWaterUse.remove(id);
            return;
        }
        nextWaterUse.computeIfAbsent(id, ignored -> farm.world().getFullTime() + randomDays(2, 3));
    }

    private void refreshComposter(FarmStructure farm) {
        if (!farm.hasComposter()) return;
        String id = farm.id();
        if (farm.composter().getBlockData() instanceof Levelled levelled
                && levelled.getLevel() >= levelled.getMaximumLevel()) {
            fertilizerEnds.computeIfAbsent(id, ignored -> farm.world().getFullTime() + randomDays(3, 5));
        } else {
            fertilizerEnds.remove(id);
        }
    }

    public void activateComposter(FarmStructure farm) {
        if (!farm.hasComposter()) return;
        if (farm.composter().getBlockData() instanceof Levelled levelled
                && levelled.getLevel() >= levelled.getMaximumLevel()) {
            fertilizerEnds.put(farm.id(), farm.world().getFullTime() + randomDays(3, 5));
            save();
        }
    }

    private long randomDays(int min, int max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1L) * DAY_TICKS;
    }

    private Block findBlock(World world, String key) {
        String[] parts = key.split(":");
        if (parts.length < 4) return null;
        try {
            UUID worldId = UUID.fromString(parts[0]);
            if (!world.getUID().equals(worldId)) return null;
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return world.getBlockAt(x, y, z);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Block findAdjacentComposter(Block cauldron) {
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.SOUTH,
                org.bukkit.block.BlockFace.EAST,
                org.bukkit.block.BlockFace.WEST}) {
            Block block = cauldron.getRelative(face);
            if (block.getType() == Material.COMPOSTER) return block;
        }
        return null;
    }

    private void load() {
        for (String key : data.getConfigurationSection("water") == null ? java.util.Set.<String>of() : data.getConfigurationSection("water").getKeys(false)) {
            nextWaterUse.put(key, data.getLong("water." + key));
        }
        for (String key : data.getConfigurationSection("fertilizer") == null ? java.util.Set.<String>of() : data.getConfigurationSection("fertilizer").getKeys(false)) {
            fertilizerEnds.put(key, data.getLong("fertilizer." + key));
        }
    }

    public void save() {
        data.set("water", null);
        data.set("fertilizer", null);
        nextWaterUse.forEach((key, value) -> data.set("water." + key, value));
        fertilizerEnds.forEach((key, value) -> data.set("fertilizer." + key, value));
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось сохранить farm-state.yml: " + exception.getMessage());
        }
    }
}
