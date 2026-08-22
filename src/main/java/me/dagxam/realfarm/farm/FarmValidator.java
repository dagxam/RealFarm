package me.dagxam.realfarm.farm;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public final class FarmValidator {
    private final int maxSize;

    public FarmValidator(int maxSize) {
        this.maxSize = Math.max(3, maxSize);
    }

    public FarmStructure findFarm(Block cropBlock) {
        World world = cropBlock.getWorld();
        int surfaceY = cropBlock.getY() - 1;
        int cropX = cropBlock.getX();
        int cropZ = cropBlock.getZ();

        for (int size = 3; size <= maxSize; size++) {
            int minStartX = cropX - (size - 2);
            int minEndX = cropX - 1;
            int minStartZ = cropZ - (size - 2);
            int minEndZ = cropZ - 1;

            for (int minX = minStartX; minX <= minEndX; minX++) {
                int maxX = minX + size - 1;
                for (int minZ = minStartZ; minZ <= minEndZ; minZ++) {
                    int maxZ = minZ + size - 1;
                    FarmStructure farm = inspectSquare(world, surfaceY, minX, maxX, minZ, maxZ);
                    if (farm != null) {
                        return farm;
                    }
                }
            }
        }
        return null;
    }

    private FarmStructure inspectSquare(World world, int y, int minX, int maxX, int minZ, int maxZ) {
        if (!isClosedBorder(world, y, minX, maxX, minZ, maxZ)) {
            return null;
        }

        Block cauldron = null;
        Block composter = null;

        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);
                Material type = block.getType();

                if (type == Material.FARMLAND) {
                    continue;
                }
                if (isCauldron(type)) {
                    if (cauldron != null) {
                        return null;
                    }
                    cauldron = block;
                    continue;
                }
                if (type == Material.COMPOSTER) {
                    if (composter != null) {
                        return null;
                    }
                    composter = block;
                    continue;
                }
                return null;
            }
        }

        if (cauldron != null && composter != null && !isAdjacent(cauldron, composter)) {
            return null;
        }

        return new FarmStructure(world, y, minX, maxX, minZ, maxZ, cauldron, composter);
    }

    private boolean isClosedBorder(World world, int y, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            if (!isBorderBlock(world.getBlockAt(x, y, minZ))) return false;
            if (!isBorderBlock(world.getBlockAt(x, y, maxZ))) return false;
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            if (!isBorderBlock(world.getBlockAt(minX, y, z))) return false;
            if (!isBorderBlock(world.getBlockAt(maxX, y, z))) return false;
        }
        return true;
    }

    private boolean isBorderBlock(Block block) {
        Material type = block.getType();
        return !type.isAir() && type != Material.FARMLAND;
    }

    private boolean isCauldron(Material type) {
        return type == Material.CAULDRON || type == Material.WATER_CAULDRON;
    }

    private boolean isAdjacent(Block first, Block second) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            if (first.getRelative(face).equals(second)) {
                return true;
            }
        }
        return false;
    }
}
