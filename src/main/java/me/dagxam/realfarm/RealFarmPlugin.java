package me.dagxam.realfarm;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealFarmPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("RealFarm включён.");
        getLogger().info("Этап 1: базовая система сельскохозяйственных культур подготовлена.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RealFarm выключен.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("realfarm")) {
            return false;
        }

        if (!sender.hasPermission("realfarm.admin")) {
            sender.sendMessage("§cУ вас нет прав для этой команды.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("§aRealFarm §7v" + getPluginMeta().getVersion());
            sender.sendMessage("§7Сельскохозяйственный плагин на Paper.");
            sender.sendMessage("§7Текущий этап: §fбазовая архитектура");
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
