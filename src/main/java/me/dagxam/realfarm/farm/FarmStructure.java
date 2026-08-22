package me.dagxam.realfarm.farm;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;

public final class FarmStructure {
    private final World world;
    private final int surfaceY;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    private final Block cauldron;
    private final Block composter;

    public FarmStructure(World world, int surfaceY, int minX, int maxX, int minZ, int maxZ, Block cauldron, Block composter) {
        this.world = world;
        this.surfaceY = surfaceY;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.cauldron = cauldron;
        this.composter = composter;
    }

    public World world() { return world; }
    public int surfaceY() { return surfaceY; }
    public int minX() { return minX; }
    public int maxX() { return maxX; }
    public int minZ() { return minZ; }
    public int maxZ() { return maxZ; }
    public Block cauldron() { return cauldron; }
    public Block composter() { return composter; }

    public boolean hasCauldron() { return cauldron != null; }
    public boolean hasComposter() { return composter != null; }

    /** Поле активно, когда есть хотя бы немного воды и хотя бы немного костной муки. */
    public boolean isActive() {
        return hasCauldron() && hasComposter() && isWatered() && hasFertilizer();
    }

    public boolean contains(int x, int z) {
        return x > minX && x < maxX && z > minZ && z < maxZ;
    }

    /** Достаточно любого количества воды: сам тип WATER_CAULDRON уже означает непустой котёл. */
    public boolean isWatered() {
        return cauldron != null && cauldron.getType() == Material.WATER_CAULDRON;
    }

    /** Достаточно любого количества костной муки в компостере. */
    public boolean hasFertilizer() {
        if (composter == null || !(composter.getBlockData() instanceof Levelled levelled)) return false;
        return levelled.getLevel() > levelled.getMinimumLevel();
    }

    public boolean isComposterFull() {
        if (composter == null || !(composter.getBlockData() instanceof Levelled levelled)) return false;
        return levelled.getLevel() >= levelled.getMaximumLevel();
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

    public String id() {
        if (cauldron != null) {
            return world.getUID() + ":" + cauldron.getX() + ":" + cauldron.getY() + ":" + cauldron.getZ();
        }
        return world.getUID() + ":" + minX + ":" + surfaceY + ":" + minZ + ":" + maxX + ":" + maxZ;
    }
}
