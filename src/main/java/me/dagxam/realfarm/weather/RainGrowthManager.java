package me.dagxam.realfarm.weather;

import me.dagxam.realfarm.farm.FarmValidator;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Отдельная природная система роста во время дождя.
 * Работает только с обычной природой и никогда не управляет участками RealFarm.
 */
public final class RainGrowthManager {
    private final FarmValidator validator;
    private final boolean enabled;
    private final int attemptsPerRun;
    private final double saplingChance;
    private final double grassChance;

    public RainGrowthManager(JavaPlugin plugin, FarmValidator validator) {
        this.validator = validator;
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("rain-growth.enabled", true);
        this.attemptsPerRun = Math.max(1, config.getInt("rain-growth.attempts-per-run", 8));
        this.saplingChance = clamp(config.getDouble("rain-growth.sapling-chance", 0.35));
        this.grassChance = clamp(config.getDouble("rain-growth.grass-chance", 0.45));
    }

    public void tick() {
        if (!enabled) return;
        for (World world : org.bukkit.Bukkit.getWorlds()) {
            if (!world.hasStorm()) continue;
            Chunk[] chunks = world.getLoadedChunks();
            if (chunks.length == 0) continue;

            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < attemptsPerRun; i++) {
                Chunk chunk = chunks[random.nextInt(chunks.length)];
                int x = (chunk.getX() << 4) + random.nextInt(16);
                int z = (chunk.getZ() << 4) + random.nextInt(16);
                Block ground = world.getHighestBlockAt(x, z);
                growNaturalBlock(ground, random);
                growNaturalBlock(ground.getRelative(BlockFace.UP), random);
            }
        }
    }

    private void growNaturalBlock(Block block, ThreadLocalRandom random) {
        if (!block.getWorld().hasStorm()) return;
        if (isFarmArea(block)) return;

        Material type = block.getType();
        if (isSapling(type) && random.nextDouble() < saplingChance) {
            TreeType treeType = toTreeType(type);
            if (treeType != null) {
                block.getWorld().generateTree(block.getLocation(), treeType);
            }
            return;
        }

        if ((type == Material.SHORT_GRASS || type == Material.FERN)
                && block.getRelative(BlockFace.UP).isEmpty()
                && random.nextDouble() < grassChance) {
            block.setType(type == Material.FERN ? Material.LARGE_FERN : Material.TALL_GRASS, false);
        }
    }

    private boolean isFarmArea(Block block) {
        Block below = block.getRelative(BlockFace.DOWN);
        if (block.getType() == Material.FARMLAND) below = block;
        return below.getType() == Material.FARMLAND && validator.findFarmAt(below) != null;
    }

    private static boolean isSapling(Material material) {
        return material == Material.OAK_SAPLING
                || material == Material.SPRUCE_SAPLING
                || material == Material.BIRCH_SAPLING
                || material == Material.JUNGLE_SAPLING
                || material == Material.ACACIA_SAPLING
                || material == Material.DARK_OAK_SAPLING
                || material == Material.CHERRY_SAPLING
                || material == Material.PALE_OAK_SAPLING;
    }

    private static TreeType toTreeType(Material material) {
        return switch (material) {
            case OAK_SAPLING -> TreeType.TREE;
            case SPRUCE_SAPLING -> TreeType.REDWOOD;
            case BIRCH_SAPLING -> TreeType.BIRCH;
            case JUNGLE_SAPLING -> TreeType.SMALL_JUNGLE;
            case ACACIA_SAPLING -> TreeType.ACACIA;
            case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
            case CHERRY_SAPLING -> TreeType.CHERRY;
            case PALE_OAK_SAPLING -> TreeType.PALE_OAK;
            default -> null;
        };
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
