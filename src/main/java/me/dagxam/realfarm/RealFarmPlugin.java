package me.dagxam.realfarm;

import me.dagxam.realfarm.farm.CropGrowthManager;
import me.dagxam.realfarm.farm.FarmStateManager;
import me.dagxam.realfarm.farm.FarmValidator;
import me.dagxam.realfarm.listener.FarmListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealFarmPlugin extends JavaPlugin {
    private FarmStateManager farmStateManager;
    private CropGrowthManager cropGrowthManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int maxFarmSize = getConfig().getInt("farm.max-size", 15);
        FarmValidator validator = new FarmValidator(maxFarmSize);
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
            sender.sendMessage("§7Активно: §fзамкнутые пашни, вода, компостер и замедленный рост.");
            sender.sendMessage("§7Рост культур: §f2–5 игровых дней.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("§aRealFarm: конфигурация перезагружена.");
            return true;
        }

        sender.sendMessage("§eИспользование: /realfarm <info|reload>");
        return true;
    }
}
