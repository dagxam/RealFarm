package me.dagxam.realfarm.farm;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Управляет ростом ванильных саженцев, посаженных непосредственно на пашню RealFarm. */
public final class TreeGrowthManager {
    private static final long DAY_TICKS = 24_000L;

    private final JavaPlugin plugin;
    private final FarmValidator validator;
    private final FarmStateManager farmStateManager;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, TreeState> trees = new HashMap<>();
    private final Map<Material, TreeType> treeTypes = new EnumMap<>(Material.class);

    public TreeGrowthManager(JavaPlugin plugin, FarmValidator validator, FarmStateManager farmStateManager) {
        this.plugin = plugin;
        this.validator = validator;
        this.farmStateManager = farmStateManager;
        this.file = new File(plugin.getDataFolder(), "tree-state.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        treeTypes.put(Material.OAK_SAPLING, TreeType.TREE);
        treeTypes.put(Material.BIRCH_SAPLING, TreeType.BIRCH);
        treeTypes.put(Material.SPRUCE_SAPLING, TreeType.REDWOOD);
        treeTypes.put(Material.JUNGLE_SAPLING, TreeType.SMALL_JUNGLE);
        treeTypes.put(Material.ACACIA_SAPLING, TreeType.ACACIA);
        treeTypes.put(Material.DARK_OAK_SAPLING, TreeType.DARK_OAK);
        load();
    }

    public boolean isSapling(Material material) {
        return treeTypes.containsKey(material);
    }

    public boolean isManaged(Block block) {
        return isSapling(block.getType())
                && block.getRelative(org.bukkit.block.BlockFace.DOWN).getType() == Material.FARMLAND
                && validator.findFarm(block) != null;
    }

    public void register(Block block) {
        if (!isManaged(block)) return;
        trees.computeIfAbsent(key(block), ignored -> createState(block));
    }

    public void unregister(Block block) {
        trees.remove(key(block));
    }

    public void tick() {
        for (String key : trees.keySet().toArray(String[]::new)) {
            TreeLocation location = parse(key);
            if (location == null) { trees.remove(key); continue; }
            World world = Bukkit.getWorld(location.worldId());
            if (world == null) continue;
            Block block = world.getBlockAt(location.x(), location.y(), location.z());
            if (!isManaged(block)) { trees.remove(key); continue; }

            FarmStructure farm = validator.findFarm(block);
            if (farm == null) { trees.remove(key); continue; }
            farmStateManager.refresh(farm);

            // Для дерева обязательна вода. Компостер ускоряет рост, но не является обязательным.
            if (!farm.isWatered()) continue;

            TreeState state = trees.get(key);
            long now = world.getFullTime();
            if (now < state.nextGrowthTick()) continue;

            TreeType treeType = treeTypes.get(block.getType());
            if (treeType == null) { trees.remove(key); continue; }
            boolean grown = world.generateTree(block.getLocation(), treeType);
            if (grown) trees.remove(key);
            else trees.put(key, new TreeState(now + retryTicks()));
        }
        save();
    }

    private TreeState createState(Block block) {
        int minDays = Math.max(2, plugin.getConfig().getInt("trees.growth-days-min", 2));
        int maxDays = Math.max(minDays, plugin.getConfig().getInt("trees.growth-days-max", 5));
        long total = ThreadLocalRandom.current().nextLong(minDays, maxDays + 1L) * DAY_TICKS;
        FarmStructure farm = validator.findFarm(block);
        if (farm != null && farm.hasFertilizer()) {
            int multiplier = Math.max(1, plugin.getConfig().getInt("fertilizer.growth-multiplier", 2));
            total = Math.max(DAY_TICKS / 4, total / multiplier);
        }
        return new TreeState(block.getWorld().getFullTime() + total);
    }

    private long retryTicks() { return 600L; }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private TreeLocation parse(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) return null;
        try {
            return new TreeLocation(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private void load() {
        ConfigurationSection section = data.getConfigurationSection("trees");
        if (section == null) return;
        for (String key : section.getKeys(false)) trees.put(key, new TreeState(section.getLong("trees." + key + ".next-growth-tick")));
    }

    public void save() {
        data.set("trees", null);
        trees.forEach((key, state) -> data.set("trees." + key + ".next-growth-tick", state.nextGrowthTick()));
        try { data.save(file); }
        catch (IOException exception) { plugin.getLogger().warning("Не удалось сохранить tree-state.yml: " + exception.getMessage()); }
    }

    private record TreeState(long nextGrowthTick) {}
    private record TreeLocation(UUID worldId, int x, int y, int z) {}
}
