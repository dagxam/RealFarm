package me.dagxam.realfarm.listener;

import me.dagxam.realfarm.farm.CropGrowthManager;
import me.dagxam.realfarm.farm.FarmItems;
import me.dagxam.realfarm.farm.FarmStateManager;
import me.dagxam.realfarm.farm.FarmStructure;
import me.dagxam.realfarm.farm.FarmValidator;
import me.dagxam.realfarm.farm.TreeGrowthManager;
import me.dagxam.realfarm.season.SeasonIntegration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class FarmListener implements Listener {
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
    private final FarmValidator validator;
    private final FarmStateManager stateManager;
    private final CropGrowthManager cropGrowthManager;
    private final TreeGrowthManager treeGrowthManager;
    private final FarmItems farmItems;
    private final SeasonIntegration seasons;
    private final Map<String, Long> messageCooldown = new HashMap<>();

    public FarmListener(FarmValidator validator, FarmStateManager stateManager, CropGrowthManager cropGrowthManager, TreeGrowthManager treeGrowthManager, FarmItems farmItems, SeasonIntegration seasons) {
        this.validator = validator; this.stateManager = stateManager; this.cropGrowthManager = cropGrowthManager;
        this.treeGrowthManager = treeGrowthManager; this.farmItems = farmItems; this.seasons = seasons;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpecialBlockPlace(BlockPlaceEvent event) { ItemStack item = event.getItemInHand(); if (farmItems.isCauldron(item)) stateManager.markFarmCauldron(event.getBlockPlaced()); else if (farmItems.isComposter(item)) stateManager.markFarmComposter(event.getBlockPlaced()); }

    @EventHandler(ignoreCancelled = true)
    public void onCropOrSaplingPlace(BlockPlaceEvent event) { Block block = event.getBlockPlaced(); if (treeGrowthManager.isManaged(block)) { treeGrowthManager.register(block); return; } if (block.getBlockData() instanceof Ageable && cropGrowthManager.isManaged(block) && validator.findFarm(block) != null) cropGrowthManager.register(block); }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) { Block block = event.getBlock(); if (stateManager.isFarmCauldron(block) || stateManager.isFarmComposter(block)) stateManager.unmarkService(block); treeGrowthManager.unregister(block); cropGrowthManager.unregister(block); }

    @EventHandler(ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent event) { Block sapling = event.getLocation().getBlock(); if (!treeGrowthManager.isManaged(sapling)) return; event.setCancelled(true); treeGrowthManager.register(sapling); }

    @EventHandler(ignoreCancelled = true)
    public void onNaturalGrow(BlockGrowEvent event) { Block block = event.getBlock(); if (!(block.getBlockData() instanceof Ageable) || !cropGrowthManager.isManaged(block)) return; FarmStructure farm = validator.findFarm(block); if (farm == null) return; event.setCancelled(true); stateManager.refresh(farm); if (!farm.isActive()) notifyInactive(farm); else cropGrowthManager.register(block); }

    @EventHandler(ignoreCancelled = true)
    public void onCropFertilize(BlockFertilizeEvent event) { Block block = event.getBlock(); if (treeGrowthManager.isManaged(block)) { event.setCancelled(true); treeGrowthManager.register(block); return; } if (block.getBlockData() instanceof Ageable && cropGrowthManager.isManaged(block) && validator.findFarm(block) != null) event.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onMoistureChange(MoistureChangeEvent event) {
        if (event.getBlock().getType() != Material.FARMLAND) return;
        FarmStructure farm = validator.findFarmAt(event.getBlock()); if (farm == null) return;
        stateManager.refresh(farm);
        if (shouldStayWet(farm)) { event.setCancelled(true); setMoisture(event.getBlock(), true); }
        else if (isWinter(farm)) { event.setCancelled(true); setMoisture(event.getBlock(), false); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onComposterUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock(); if (block == null || !stateManager.isFarmComposter(block)) return;
        FarmStructure farm = findFarmForService(block); if (farm == null) return;
        ItemStack item = event.getItem(); event.setUseInteractedBlock(Event.Result.DENY);
        if (item == null || item.getType() == Material.AIR) return; event.setUseItemInHand(Event.Result.DENY);
        if (item.getType() != Material.BONE_MEAL) { event.getPlayer().sendMessage("§eКомпостер фермы заполняется только костной мукой."); return; }
        if (!(block.getBlockData() instanceof Levelled levelled)) return;
        if (levelled.getLevel() < levelled.getMaximumLevel()) { levelled.setLevel(levelled.getLevel() + 1); block.setBlockData(levelled); consumeOne(event.getPlayer(), item); }
        stateManager.activateComposter(farm); updateFarmSoil(farm);
    }

    public void tick() { for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) { showTargetInfo(player); refreshNearbyFarmSoil(player); } }

    private FarmStructure findFarmForService(Block service) { for (BlockFace face : SIDES) { Block neighbour = service.getRelative(face); if (neighbour.getType() != Material.FARMLAND) continue; FarmStructure farm = validator.findFarmAt(neighbour); if (farm != null && (service.equals(farm.cauldron()) || service.equals(farm.composter()))) return farm; } return null; }

    private void showTargetInfo(Player player) {
        Block target = player.getTargetBlockExact(6); if (target == null) return;
        FarmStructure farm = null; String type = null;
        if (stateManager.isFarmCauldron(target)) { farm = findFarmForService(target); type = "котёл"; }
        if (stateManager.isFarmComposter(target)) { farm = findFarmForService(target); type = "компостер"; }
        if (farm == null || type == null) return;
        stateManager.refresh(farm); int plot = stateManager.getPlotNumber(farm); int planted = plantedCount(farm);
        String own = type.equals("котёл") ? (farm.isWatered() ? "§aВода: " + farm.waterLevel() + "/" + farm.waterMaximumLevel() : "§cВода: нет") : (farm.hasFertilizer() ? "§aКостная мука: " + farm.fertilizerLevel() + "/" + farm.fertilizerMaximumLevel() : "§cКостная мука: нет");
        String status = farm.isActive() ? "§aАКТИВНО" : "§cНЕ АКТИВНО";
        String text = "§6Участок №" + plot + " §7| §fПашня: §e" + farm.farmlandCount() + " §7| §fПосажено: §e" + planted + " §7| " + own + " §7| " + status;
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }

    private int plantedCount(FarmStructure farm) { int count = 0; for (FarmStructure.BlockPosition position : farm.farmland()) { Block planted = farm.world().getBlockAt(position.x(), position.y() + 1, position.z()); if ((planted.getBlockData() instanceof Ageable && cropGrowthManager.isManaged(planted)) || treeGrowthManager.isManaged(planted)) count++; } return count; }

    private void refreshNearbyFarmSoil(Player player) {
        int radius = 8; Map<String, FarmStructure> farms = new HashMap<>(); int cx = player.getLocation().getBlockX(), cy = player.getLocation().getBlockY(), cz = player.getLocation().getBlockZ();
        for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) for (int y = cy - 2; y <= cy + 2; y++) { Block block = player.getWorld().getBlockAt(x, y, z); if (block.getType() == Material.FARMLAND) { FarmStructure farm = validator.findFarmAt(block); if (farm != null) farms.putIfAbsent(farm.id(), farm); } }
        for (FarmStructure farm : farms.values()) { stateManager.refresh(farm); updateFarmSoil(farm); }
    }

    private void updateFarmSoil(FarmStructure farm) {
        boolean wet = shouldStayWet(farm);
        for (FarmStructure.BlockPosition position : farm.farmland()) setMoisture(farm.world().getBlockAt(position.x(), position.y(), position.z()), wet);
    }

    private boolean shouldStayWet(FarmStructure farm) {
        if (farm.world().hasStorm()) return true;
        SeasonIntegration.Season season = seasons.getSeason(farm.world());
        if (season == SeasonIntegration.Season.WINTER || season == SeasonIntegration.Season.FALL) return false;
        return farm.isWatered();
    }

    private boolean isWinter(FarmStructure farm) { return seasons.getSeason(farm.world()) == SeasonIntegration.Season.WINTER; }

    private void setMoisture(Block block, boolean wet) { if (!(block.getBlockData() instanceof Farmland farmland)) return; farmland.setMoisture(wet ? farmland.getMaximumMoisture() : 0); block.setBlockData(farmland, false); }
    private void consumeOne(Player player, ItemStack item) { if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1); }

    private void notifyInactive(FarmStructure farm) { String reason = !farm.hasCauldron() ? "§cНет котла фермы рядом с пашней." : !farm.hasComposter() ? "§cНет компостера фермы рядом с пашней." : !farm.isWatered() ? "§cНет воды в котле фермы." : "§cНет костной муки в компостере фермы."; long now = System.currentTimeMillis(), last = messageCooldown.getOrDefault(farm.id(), 0L); if (now - last < 5000L) return; messageCooldown.put(farm.id(), now); for (Player player : farm.world().getPlayers()) if (player.getLocation().distanceSquared(farm.world().getBlockAt(farm.minX(), farm.farmland().iterator().next().y(), farm.minZ()).getLocation()) < 20 * 20) player.sendMessage(reason); }
}
