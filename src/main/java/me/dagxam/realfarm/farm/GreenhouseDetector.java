package me.dagxam.realfarm.farm;

import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Проверяет, находится ли участок RealFarm внутри полностью закрытого стеклянного парника.
 * Разрешены обычное стекло, цветное стекло, стеклянные панели и цветные стеклянные панели.
 */
public final class GreenhouseDetector {
    private static final int MAX_ROOF_HEIGHT = 8;

    public boolean isGreenhouse(FarmStructure farm) {
        if (farm.farmland().isEmpty()) return false;

        int groundY = farm.farmland().iterator().next().y();
        for (int roofY = groundY + 2; roofY <= groundY + MAX_ROOF_HEIGHT; roofY++) {
            if (hasCompleteGlassRoof(farm, roofY) && hasCompleteGlassWalls(farm, groundY + 1, roofY - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompleteGlassRoof(FarmStructure farm, int roofY) {
        for (int x = farm.minX(); x <= farm.maxX(); x++) {
            for (int z = farm.minZ(); z <= farm.maxZ(); z++) {
                if (!isGlass(farm.world().getBlockAt(x, roofY, z))) return false;
            }
        }
        return true;
    }

    private boolean hasCompleteGlassWalls(FarmStructure farm, int fromY, int toY) {
        int minX = farm.minX() - 1;
        int maxX = farm.maxX() + 1;
        int minZ = farm.minZ() - 1;
        int maxZ = farm.maxZ() + 1;

        for (int y = fromY; y <= toY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!isGlass(farm.world().getBlockAt(x, y, minZ))) return false;
                if (!isGlass(farm.world().getBlockAt(x, y, maxZ))) return false;
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                if (!isGlass(farm.world().getBlockAt(minX, y, z))) return false;
                if (!isGlass(farm.world().getBlockAt(maxX, y, z))) return false;
            }
        }
        return true;
    }

    private boolean isGlass(Block block) {
        String name = block.getType().name();
        return name.equals("GLASS") || name.equals("GLASS_PANE")
                || name.endsWith("_STAINED_GLASS") || name.endsWith("_STAINED_GLASS_PANE");
    }
}
