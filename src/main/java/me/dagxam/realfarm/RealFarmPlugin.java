package me.dagxam.realfarm;

import me.dagxam.realfarm.crop.CropRegistry;
import me.dagxam.realfarm.farm.CropGrowthManager;
import me.dagxam.realfarm.farm.FarmStateManager;
import me.dagxam.realfarm.farm.FarmStructure;
import me.dagxam.realfarm.farm.FarmValidator;
import me.dagxam.realfarm.listener.FarmListener;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealFarmPlugin extends JavaPlugin {
    private FarmValidator validator;
    private FarmStateManager farmStateManager;
    private CropRegistry cropRegistry;
    private CropGrowthManager cropGrowthManager;
    private FarmListener farmListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createManagers();

        farmListener = new FarmListener(validator, farmStateManager, cropGrowthManager);
        getServer().getPluginManager().registerEvents(farmListener, this);

        getServer().getScheduler().runTaskTimer(this, farmStateManager::tick, 200L, 200L);
        getServer().getScheduler().runTaskTimer(this, cropGrowthManager::tick, 100L, 100L);
        getServer().getScheduler().runTaskTimer(this, farmListener::tick, 20L, 20L);

        getLogger().info("RealFarm включён.");
        getLogger().info("Система замкнутых пашен, котлов, воды и компостеров активна.");
        getLogger().info("Зарегистрировано культур: " + cropRegistry.enabled().size());
    }

    private void createManagers() {
        int minFarmSize = getConfig().getInt("farm.min-size", 3);
        int maxFarmSize = getConfig().getInt("farm.max-size", 15);
        validator = new FarmValidator(minFarmSize, maxFarmSize);
        farmStateManager = new FarmStateManager(this);
        cropRegistry = new CropRegistry(getConfig());
        cropGrowthManager = new CropGrowthManager(this, validator, farmStateManager, cropRegistry);
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
            sender.sendMessage("§7Активное поле требует: §fкотёл с полной водой + полный компостер с костной мукой.");
            sender.sendMessage("§7Без одного из двух ресурсов поле не активно и пашня высыхает.");
            sender.sendMessage("§7Зарегистрировано культур: §f" + cropRegistry.enabled().size());
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
            Block interior = target.getBlockData() instanceof Ageable
                    ? target.getRelative(org.bukkit.block.BlockFace.DOWN)
                    : target;
            FarmStructure farm = validator.findFarmAt(interior);
            if (farm == null) {
                sender.sendMessage("§cЗамкнутая пашня не найдена.");
                return true;
            }

            farmStateManager.refresh(farm);
            sender.sendMessage("§aПашня найдена: §f" + (farm.maxX() - farm.minX() + 1) + "×" + (farm.maxZ() - farm.minZ() + 1));
            sender.sendMessage("§7Котёл: " + (farm.hasCauldron() ? "§aесть" : "§cнет"));
            sender.sendMessage("§7Вода: " + (farm.isWatered() ? "§aполная" : "§cнет"));
            sender.sendMessage("§7Компостер: " + (farm.hasComposter() ? (farm.isComposterFull() ? "§aполный" : "§cне заполнен") : "§cне установлен"));
            sender.sendMessage("§7Состояние поля: " + (farm.isActive() ? "§aАКТИВНО" : "§cНЕ АКТИВНО"));
            return true;
        }

        if (args[0].equalsIgnoreCase("crops")) {
            sender.sendMessage("§aКультуры RealFarm:");
            cropRegistry.enabled().forEach(crop -> sender.sendMessage("§7- §f" + crop.displayName() + " §8(" + crop.id() + ")"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("§aКонфигурация перечитана. §eДля применения размеров пашни и списка культур перезапустите плагин/сервер.");
            return true;
        }

        sender.sendMessage("§eИспользование: /realfarm <info|status|crops|reload>");
        return true;
    }
}
