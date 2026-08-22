package me.dagxam.realfarm.farm;

import me.dagxam.realfarm.crop.CropRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Управляет стадиями роста культур, зарегистрированных в CropRegistry.
 * Обычный случайный рост Minecraft для культур внутри RealFarm отменяется.
 */
public final class CropGrowthManager {
    private static final long DAY_TICKS = 24_000L;

    private final JavaPlugin plugin;
    private final FarmValidator validator;
    private final FarmStateManager farmStateManager;
    private final CropRegistry cropRegistry;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, CropState> crops = new HashMap<>();

    public CropGrowthManager(JavaPlugin plugin, FarmValidator validator, FarmStateManager farmStateManager, CropRegistry cropRegistry) {
        this.plugin = plugin;
        this.validator = validator;
        this.farmStateManager = farmStateManager;
        this.cropRegistry = cropRegistry;
        this.file = new File(plugin.getDataFolder(), "crop-state.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public boolean isManaged(Block block) {
        return cropRegistry.isManaged(block.getType());
    }

    public void register(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (block.getRelative(org.bukkit.block.BlockFace.DOWN).getType() != Material.FARMLAND) return;
        if (!cropRegistry.isManaged(block.getType())) return;
        if (validator.findFarm(block) == null) return;
        if (ageable.getAge() >= ageable.getMaximumAge()) return;

        crops.computeIfAbsent(key(block), ignored -> createState(block, ageable));
    }

    public void unregister(Block block) {
        crops.remove(key(block));
    }

    private CropState createState(Block block, Ageable ageable) {
        int minDays = Math.max(1, plugin.getConfig().getInt("crops.growth-days-min", 2));
        int maxDays = Math.max(minDays, plugin.getConfig().getInt("crops.growth-days-max", 5));
        long totalTicks = ThreadLocalRandom.current().nextLong(minDays, maxDays + 1L) * DAY_TICKS;
        long stageTicks = Math.max(200L, totalTicks / Math.max(1, ageable.getMaximumAge()));
        return new CropState(totalTicks, stageTicks, block.getWorld().getFullTime() + stageTicks);
    }

    public void tick() {
        for (String key : crops.keySet().toArray(String[]::new)) {
            CropLocation location = parse(key);
            if (location == null) {
                crops.remove(key);
                continue;
            }

            World world = Bukkit.getWorld(location.worldId());
            if (world == null) continue;

            Block block = world.getBlockAt(location.x(), location.y(), location.z());
            if (!(block.getBlockData() instanceof Ageable ageable) || !cropRegistry.isManaged(block.getType())) {
                crops.remove(key);
                continue;
            }

            FarmStructure farm = validator.findFarm(block);
            if (farm == null || !farm.hasCauldron() || !farm.isWatered()) continue;

            farmStateManager.refresh(farm);
            CropState state = crops.get(key);
            if (state == null) continue;
            long now = world.getFullTime();
            if (now < state.nextGrowthTick()) continue;

            boolean fertilizer = farmStateManager.isFertilizerActive(farm);
            int growth = fertilizer ? Math.max(1, plugin.getConfig().getInt("fertilizer.growth-multiplier", 2)) : 1;
            int newAge = Math.min(ageable.getMaximumAge(), ageable.getAge() + growth);
            ageable.setAge(newAge);
            block.setBlockData(ageable);

            if (newAge >= ageable.getMaximumAge()) {
                crops.remove(key);
                continue;
            }

            long nextInterval = fertilizer ? Math.max(100L, state.stageTicks() / growth) : state.stageTicks();
            crops.put(key, new CropState(state.totalTicks(), state.stageTicks(), now + nextInterval));
        }
        save();
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private CropLocation parse(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        try {
            return new CropLocation(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void load() {
        ConfigurationSection section = data.getConfigurationSection("crops");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            long total = section.getLong(key + ".total-ticks");
            long stage = section.getLong(key + ".stage-ticks");
            long next = section.getLong(key + ".next-growth-tick");
            crops.put(key, new CropState(total, stage, next));
        }
    }

    public void save() {
        data.set("crops", null);
        crops.forEach((key, state) -> {
            data.set("crops." + key + ".total-ticks", state.totalTicks());
            data.set("crops." + key + ".stage-ticks", state.stageTicks());
            data.set("crops." + key + ".next-growth-tick", state.nextGrowthTick());
        });
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось сохранить crop-state.yml: " + exception.getMessage());
        }
    }

    private record CropState(long totalTicks, long stageTicks, long nextGrowthTick) {}
    private record CropLocation(UUID worldId, int x, int y, int z) {}
}
