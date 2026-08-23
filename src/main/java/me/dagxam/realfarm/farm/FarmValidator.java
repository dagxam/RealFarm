package me.dagxam.realfarm.farm;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/** Ищет связный участок обычной пашни любой формы. */
public final class FarmValidator {
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
    private final int maxBlocks;
    private final FarmStateManager stateManager;

    public FarmValidator(int minSize, int maxSize, FarmStateManager stateManager) {
        this.maxBlocks = Math.max(64, Math.max(minSize, maxSize) * Math.max(minSize, maxSize));
        this.stateManager = stateManager;
    }

    public FarmStructure findFarm(Block cropBlock) {
        return findFarmAt(cropBlock.getRelative(BlockFace.DOWN));
    }

    public FarmStructure findFarmAt(Block start) {
        if (start.getType() != Material.FARMLAND) return null;

        Set<FarmStructure.BlockPosition> farmland = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(start);

        Block cauldron = null;
        Block composter = null;

        while (!queue.isEmpty()) {
            Block block = queue.poll();
            FarmStructure.BlockPosition position = new FarmStructure.BlockPosition(block.getX(), block.getY(), block.getZ());
            if (!farmland.add(position)) continue;
            if (farmland.size() > maxBlocks) return null;

            for (BlockFace face : SIDES) {
                Block neighbour = block.getRelative(face);
                if (neighbour.getType() == Material.FARMLAND) {
                    FarmStructure.BlockPosition next = new FarmStructure.BlockPosition(neighbour.getX(), neighbour.getY(), neighbour.getZ());
                    if (!farmland.contains(next)) queue.add(neighbour);
                    continue;
                }
                if (stateManager.isFarmCauldron(neighbour)) {
                    if (cauldron != null && !cauldron.equals(neighbour)) return null;
                    cauldron = neighbour;
                }
                if (stateManager.isFarmComposter(neighbour)) {
                    if (composter != null && !composter.equals(neighbour)) return null;
                    composter = neighbour;
                }
            }
        }

        return new FarmStructure(start.getWorld(), farmland, cauldron, composter);
    }
}
