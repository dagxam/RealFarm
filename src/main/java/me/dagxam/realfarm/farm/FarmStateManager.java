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

/** Хранит специальные блоки RealFarm, номера участков и расход ресурсов. */
public final class FarmStateManager {
    private static final long DAY_TICKS = 24_000L;

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Long> nextWaterUse = new HashMap<>();
    private final Map<String, Long> fertilizerEnds = new HashMap<>();
    private final Map<String, Integer> plotNumbers = new HashMap<>();
    private final Map<String, String> serviceBlocks = new HashMap<>();

    public FarmStateManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "farm-state.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public void markFarmCauldron(Block block) { serviceBlocks.put(FarmStructure.locationKey(block), "cauldron"); save(); }
    public void markFarmComposter(Block block) { serviceBlocks.put(FarmStructure.locationKey(block), "composter"); save(); }
    public void unmarkService(Block block) {
        String key = FarmStructure.locationKey(block);
        serviceBlocks.remove(key);
        nextWaterUse.remove(key);
        fertilizerEnds.remove(key);
        save();
    }

    public boolean isFarmCauldron(Block block) {
        return (block.getType() == Material.CAULDRON || block.getType() == Material.WATER_CAULDRON)
                && "cauldron".equals(serviceBlocks.get(FarmStructure.locationKey(block)));
    }

    public boolean isFarmComposter(Block block) {
        return block.getType() == Material.COMPOSTER
                && "composter".equals(serviceBlocks.get(FarmStructure.locationKey(block)));
    }

    public int getPlotNumber(FarmStructure farm) {
        return plotNumbers.computeIfAbsent(farm.id(), ignored -> plotNumbers.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1);
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
                if (nextWaterUse.get(key) <= now) {
                    Block block = findBlock(world, key);
                    if (block != null && block.getType() == Material.WATER_CAULDRON) block.setType(Material.CAULDRON);
                    nextWaterUse.remove(key);
                }
            }
            for (String key : fertilizerEnds.keySet().toArray(String[]::new)) {
                if (!key.startsWith(world.getUID().toString() + ":")) continue;
                if (fertilizerEnds.get(key) <= now) {
                    Block block = findBlock(world, key);
                    if (block != null && isFarmComposter(block) && block.getBlockData() instanceof Levelled levelled) {
                        levelled.setLevel(levelled.getMinimumLevel());
                        block.setBlockData(levelled);
                    }
                    fertilizerEnds.remove(key);
                }
            }
        }
        save();
    }

    private void refreshWater(FarmStructure farm) {
        if (!farm.hasCauldron()) return;
        String key = FarmStructure.locationKey(farm.cauldron());
        if (!farm.isWatered()) { nextWaterUse.remove(key); return; }
        nextWaterUse.computeIfAbsent(key, ignored -> farm.world().getFullTime() + randomDays("water.consume-after-days-min", 2, "water.consume-after-days-max", 3));
    }

    private void refreshComposter(FarmStructure farm) {
        if (!farm.hasComposter()) return;
        String key = FarmStructure.locationKey(farm.composter());
        if (farm.hasFertilizer()) fertilizerEnds.computeIfAbsent(key, ignored -> farm.world().getFullTime() + randomDays("fertilizer.duration-days-min", 3, "fertilizer.duration-days-max", 5));
        else fertilizerEnds.remove(key);
    }

    public void activateComposter(FarmStructure farm) { refreshComposter(farm); save(); }

    private long randomDays(String minPath, int minDefault, String maxPath, int maxDefault) {
        int min = Math.max(1, plugin.getConfig().getInt(minPath, minDefault));
        int max = Math.max(min, plugin.getConfig().getInt(maxPath, maxDefault));
        return ThreadLocalRandom.current().nextLong(min, max + 1L) * DAY_TICKS;
    }

    private Block findBlock(World world, String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        try {
            if (!world.getUID().equals(UUID.fromString(parts[0]))) return null;
            return world.getBlockAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private void load() {
        if (data.getConfigurationSection("water") != null) for (String key : data.getConfigurationSection("water").getKeys(false)) nextWaterUse.put(key, data.getLong("water." + key));
        if (data.getConfigurationSection("fertilizer") != null) for (String key : data.getConfigurationSection("fertilizer").getKeys(false)) fertilizerEnds.put(key, data.getLong("fertilizer." + key));
        if (data.getConfigurationSection("plots") != null) for (String key : data.getConfigurationSection("plots").getKeys(false)) plotNumbers.put(key, data.getInt("plots." + key));
        if (data.getConfigurationSection("service-blocks") != null) for (String key : data.getConfigurationSection("service-blocks").getKeys(false)) serviceBlocks.put(key, data.getString("service-blocks." + key));
    }

    public void save() {
        data.set("water", null); data.set("fertilizer", null); data.set("plots", null); data.set("service-blocks", null);
        nextWaterUse.forEach((key, value) -> data.set("water." + key, value));
        fertilizerEnds.forEach((key, value) -> data.set("fertilizer." + key, value));
        plotNumbers.forEach((key, value) -> data.set("plots." + key, value));
        serviceBlocks.forEach((key, value) -> data.set("service-blocks." + key, value));
        try { data.save(file); } catch (IOException exception) { plugin.getLogger().warning("Не удалось сохранить farm-state.yml: " + exception.getMessage()); }
    }
}
