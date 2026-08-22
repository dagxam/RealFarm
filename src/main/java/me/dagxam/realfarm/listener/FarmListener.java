package me.dagxam.realfarm.listener;

import me.dagxam.realfarm.farm.CropGrowthManager;
import me.dagxam.realfarm.farm.FarmStateManager;
import me.dagxam.realfarm.farm.FarmStructure;
import me.dagxam.realfarm.farm.FarmValidator;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class FarmListener implements Listener {
    private final FarmValidator validator;
    private final FarmStateManager farmStateManager;
    private final CropGrowthManager cropGrowthManager;
    private final Map<String, Long> messageCooldown = new HashMap<>();

    public FarmListener(FarmValidator validator, FarmStateManager farmStateManager, CropGrowthManager cropGrowthManager) {
        this.validator = validator;
        this.farmStateManager = farmStateManager;
        this.cropGrowthManager = cropGrowthManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCropPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!(block.getBlockData() instanceof Ageable)) return;
        if (block.getRelative(org.bukkit.block.BlockFace.DOWN).getType() != Material.FARMLAND) return;
        if (!cropGrowthManager.isManaged(block)) return;

        FarmStructure farm = validator.findFarm(block);
        if (farm == null) return;
        cropGrowthManager.register(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        cropGrowthManager.unregister(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onNaturalGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Ageable)) return;
        if (block.getRelative(org.bukkit.block.BlockFace.DOWN).getType() != Material.FARMLAND) return;
        if (!cropGrowthManager.isManaged(block)) return;

        FarmStructure farm = validator.findFarm(block);
        if (farm == null) return;

        event.setCancelled(true);
        if (!farm.hasCauldron()) {
            notifyNearby(farm, "§cПашня не готова: §7внутри замкнутого квадрата нужен котёл.");
            return;
        }
        if (!farm.isWatered()) {
            notifyNearby(farm, "§cНет воды: §7полностью заполните котёл водой.");
            return;
        }

        farmStateManager.refresh(farm);
        cropGrowthManager.register(block);
    }

    /**
     * На ферме костная мука должна использоваться через компостер,
     * поэтому прямое мгновенное удобрение культуры отключается.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCropFertilize(BlockFertilizeEvent event) {
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Ageable)) return;
        if (!cropGrowthManager.isManaged(block)) return;
        if (validator.findFarm(block) == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onComposterUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.COMPOSTER) return;

        FarmStructure farm = validator.findFarmAt(block);
        if (farm == null || farm.composter() == null || !farm.composter().equals(block) || !farm.hasCauldron()) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            event.setUseInteractedBlock(Event.Result.DENY);
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        if (item.getType() != Material.BONE_MEAL) {
            event.getPlayer().sendMessage("§eКомпостер этой пашни заполняется только костной мукой.");
            return;
        }

        if (!(block.getBlockData() instanceof Levelled levelled)) return;
        if (levelled.getLevel() >= levelled.getMaximumLevel()) {
            event.getPlayer().sendMessage("§aКомпостер уже полностью заполнен и ускоряет рост.");
            farmStateManager.activateComposter(farm);
            return;
        }

        levelled.setLevel(levelled.getLevel() + 1);
        block.setBlockData(levelled);
        consumeOne(event.getPlayer(), item);

        if (levelled.getLevel() >= levelled.getMaximumLevel()) {
            farmStateManager.activateComposter(farm);
            event.getPlayer().sendMessage("§aКомпостер полностью заполнен. Рост растений ускорен.");
        } else {
            event.getPlayer().sendMessage("§aКостная мука добавлена: §f" + levelled.getLevel() + "/" + levelled.getMaximumLevel());
        }
    }

    private void consumeOne(Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        item.setAmount(item.getAmount() - 1);
    }

    private void notifyNearby(FarmStructure farm, String message) {
        long now = System.currentTimeMillis();
        long last = messageCooldown.getOrDefault(farm.id(), 0L);
        if (now - last < 5_000L) return;
        messageCooldown.put(farm.id(), now);

        double centerX = (farm.minX() + farm.maxX()) / 2.0 + 0.5;
        double centerZ = (farm.minZ() + farm.maxZ()) / 2.0 + 0.5;
        for (Player player : farm.world().getPlayers()) {
            if (Math.abs(player.getLocation().getX() - centerX) <= 12
                    && Math.abs(player.getLocation().getZ() - centerZ) <= 12
                    && Math.abs(player.getLocation().getY() - farm.surfaceY()) <= 8) {
                player.sendMessage(message);
            }
        }
    }
}
