package me.dagxam.realfarm;

import me.dagxam.realfarm.farm.CropGrowthManager;
import me.dagxam.realfarm.farm.FarmStateManager;
import me.dagxam.realfarm.farm.FarmStructure;
import me.dagxam.realfarm.farm.FarmValidator;
import me.dagxam.realfarm.listener.FarmListener;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealFarmPlugin extends JavaPlugin {
    private FarmValidator validator;
    private FarmStateManager farmStateManager;
    private CropGrowthManager cropGrowthManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int minFarmSize = getConfig().getInt("farm.min-size", 3);
        int maxFarmSize = getConfig().getInt("farm.max-size", 15);
        validator = new FarmValidator(minFarmSize, maxFarmSize);
        farmStateManager = new FarmStateManager(this);
        cropGrowthManager = new CropGrowthManager(this, validator, farmStateManager);

        getServer().getPluginManager().registerEvents(
                new FarmListener(validator, farmStateManager, cropGrowthManager), this
        );

        getServer().getScheduler().runTaskTimer(this, farmStateManager::tick, 200L, 200L);
        getServer().getScheduler().runTaskTimer(this, cropGrowthManager::tick, 100L, 100L);

        getLogger().info("RealFarm включён.");
        getLogger().info("Система замкнутых пашен, котлов, воды и компостеров активна.");
    }

    @Override
    public void onDisable() {
        if (farmStateManager != null) farmStateManager.save();
        if (cropGrowthManager != null) cropGrowthManager.save();
        getLogger().info("RealFarm выключен.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("realfarm")) return false;

        if (!sender.hasPermission("realfarm.admin")) {
            sender.sendMessage("§cУ вас нет прав для этой команды.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("§aRealFarm §7v" + getPluginMeta().getVersion());
            sender.sendMessage("§7Активно: §fзамкнутые пашни, вода, компостер и контролируемый рост.");
            sender.sendMessage("§7Рост культур: §f2–5 игровых дней.");
            sender.sendMessage("§7Проверка пашни: §f/realfarm status");
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cЭту команду можно использовать только в игре.");
                return true;
            }
            Block target = player.getTargetBlockExact(10);
            if (target == null) {
                sender.sendMessage("§eПосмотрите на блок внутри пашни и повторите команду.");
                return true;
            }
            FarmStructure farm = validator.findFarmAt(target.getType().isAir() ? target.getRelative(org.bukkit.block.BlockFace.DOWN) : target);
            if (farm == null) {
                sender.sendMessage("§cЗамкнутая пашня не найдена.");
                return true;
            }

            sender.sendMessage("§aПашня найдена: §f" + (farm.maxX() - farm.minX() + 1) + "×" + (farm.maxZ() - farm.minZ() + 1));
            sender.sendMessage("§7Котёл: " + (farm.hasCauldron() ? "§aесть" : "§cнет"));
            sender.sendMessage("§7Вода: " + (farm.isWatered() ? "§aесть" : "§cнет"));
            sender.sendMessage("§7Компостер: " + (farm.hasComposter() ? (farm.isComposterFull() ? "§aполный" : "§eесть, но не полный") : "§7не установлен"));
            sender.sendMessage("§7Ускорение роста: " + (farmStateManager.isFertilizerActive(farm) ? "§aактивно" : "§7не активно"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("§aRealFarm: конфигурация перезагружена.");
            return true;
        }

        sender.sendMessage("§eИспользование: /realfarm <info|status|reload>");
        return true;
    }
}
