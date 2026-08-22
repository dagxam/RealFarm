package me.dagxam.realfarm.farm;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

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

    public boolean hasCauldron() {
        return cauldron != null;
    }

    public boolean hasComposter() {
        return composter != null;
    }

    public boolean contains(int x, int z) {
        return x > minX && x < maxX && z > minZ && z < maxZ;
    }

    public boolean isWatered() {
        return cauldron != null && cauldron.getType() == Material.WATER_CAULDRON;
    }

    public String id() {
        if (cauldron != null) {
            return world.getUID() + ":" + cauldron.getX() + ":" + cauldron.getY() + ":" + cauldron.getZ();
        }
        return world.getUID() + ":" + minX + ":" + surfaceY + ":" + minZ + ":" + maxX + ":" + maxZ;
    }
}
