package me.dagxam.realfarm.farm;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Описание одного участка RealFarm. Форма участка может быть любой, главное — связная пашня. */
public final class FarmStructure {
    private final World world;
    private final Set<BlockPosition> farmland;
    private final Block cauldron;
    private final Block composter;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    public FarmStructure(World world, Set<BlockPosition> farmland, Block cauldron, Block composter) {
        this.world = world;
        this.farmland = Collections.unmodifiableSet(new HashSet<>(farmland));
        this.cauldron = cauldron;
        this.composter = composter;

        int localMinX = Integer.MAX_VALUE, localMaxX = Integer.MIN_VALUE;
        int localMinZ = Integer.MAX_VALUE, localMaxZ = Integer.MIN_VALUE;
        for (BlockPosition position : farmland) {
            localMinX = Math.min(localMinX, position.x());
            localMaxX = Math.max(localMaxX, position.x());
            localMinZ = Math.min(localMinZ, position.z());
            localMaxZ = Math.max(localMaxZ, position.z());
        }
        this.minX = localMinX;
        this.maxX = localMaxX;
        this.minZ = localMinZ;
        this.maxZ = localMaxZ;
    }

    public World world() { return world; }
    public Set<BlockPosition> farmland() { return farmland; }
    public Block cauldron() { return cauldron; }
    public Block composter() { return composter; }
    public int minX() { return minX; }
    public int maxX() { return maxX; }
    public int minZ() { return minZ; }
    public int maxZ() { return maxZ; }

    public boolean hasCauldron() { return cauldron != null; }
    public boolean hasComposter() { return composter != null; }
    public int farmlandCount() { return farmland.size(); }

    /** Участок работает только при наличии обоих специальных блоков и ресурсов в них. */
    public boolean isActive() {
        return hasCauldron() && hasComposter() && isWatered() && hasFertilizer();
    }

    public boolean contains(Block block) {
        return block.getWorld().equals(world)
                && farmland.contains(new BlockPosition(block.getX(), block.getY(), block.getZ()));
    }

    public boolean isWatered() {
        return cauldron != null && cauldron.getType() == Material.WATER_CAULDRON;
    }

    public boolean hasFertilizer() {
        if (composter == null || !(composter.getBlockData() instanceof Levelled levelled)) return false;
        return levelled.getLevel() > levelled.getMinimumLevel();
    }

    public int waterLevel() {
        if (cauldron == null || !(cauldron.getBlockData() instanceof Levelled levelled)) return 0;
        return levelled.getLevel();
    }

    public int waterMaximumLevel() {
        if (cauldron == null || !(cauldron.getBlockData() instanceof Levelled levelled)) return 0;
        return levelled.getMaximumLevel();
    }

    public int fertilizerLevel() {
        if (composter == null || !(composter.getBlockData() instanceof Levelled levelled)) return 0;
        return levelled.getLevel();
    }

    public int fertilizerMaximumLevel() {
        if (composter == null || !(composter.getBlockData() instanceof Levelled levelled)) return 0;
        return levelled.getMaximumLevel();
    }

    /** Стабильный ключ участка: в первую очередь координаты специального котла. */
    public String id() {
        Block anchor = cauldron != null ? cauldron : composter;
        if (anchor != null) return locationKey(anchor);
        return world.getUID() + ":empty:" + minX + ":" + minZ + ":" + farmland.size();
    }

    public static String locationKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    public record BlockPosition(int x, int y, int z) {}
}
