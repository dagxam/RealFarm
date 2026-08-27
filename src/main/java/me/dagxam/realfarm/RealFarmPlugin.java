package me.dagxam.realfarm;

import me.dagxam.realfarm.crop.CropRegistry;
import me.dagxam.realfarm.farm.CropGrowthManager;
import me.dagxam.realfarm.farm.FarmItems;
import me.dagxam.realfarm.farm.FarmStateManager;
import me.dagxam.realfarm.farm.FarmStructure;
import me.dagxam.realfarm.farm.FarmValidator;
import me.dagxam.realfarm.farm.TreeGrowthManager;
import me.dagxam.realfarm.listener.FarmListener;
import me.dagxam.realfarm.season.SeasonIntegration;
import me.dagxam.realfarm.weather.RainGrowthManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealFarmPlugin extends JavaPlugin {
    private FarmValidator validator; private FarmStateManager farmStateManager; private CropRegistry cropRegistry;
    private CropGrowthManager cropGrowthManager; private TreeGrowthManager treeGrowthManager; private FarmListener farmListener;
    private FarmItems farmItems; private SeasonIntegration seasonIntegration; private RainGrowthManager rainGrowthManager;

    @Override public void onEnable() {
        saveDefaultConfig(); createManagers(); registerRecipes();
        farmListener = new FarmListener(validator, farmStateManager, cropGrowthManager, treeGrowthManager, farmItems, seasonIntegration);
        getServer().getPluginManager().registerEvents(farmListener, this);
        getServer().getScheduler().runTaskTimer(this, farmStateManager::tick, 200L, 200L);
        getServer().getScheduler().runTaskTimer(this, cropGrowthManager::tick, 100L, 100L);
        getServer().getScheduler().runTaskTimer(this, treeGrowthManager::tick, 100L, 100L);
        getServer().getScheduler().runTaskTimer(this, farmListener::tick, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, rainGrowthManager::tick, 200L, 200L);
        getLogger().info("RealFarm включён. Участки, культуры, деревья, сезонная влажность и природный рост во время дождя активны.");
    }

    private void createManagers() {
        farmStateManager = new FarmStateManager(this); validator = new FarmValidator(getConfig().getInt("farm.min-size", 3), getConfig().getInt("farm.max-size", 15), farmStateManager);
        cropRegistry = new CropRegistry(getConfig()); cropGrowthManager = new CropGrowthManager(this, validator, farmStateManager, cropRegistry);
        treeGrowthManager = new TreeGrowthManager(this, validator, farmStateManager); farmItems = new FarmItems(this); seasonIntegration = new SeasonIntegration(this);
        rainGrowthManager = new RainGrowthManager(this, validator);
    }

    private void registerRecipes() {
        NamespacedKey cauldronKey = new NamespacedKey(this, "farm_cauldron_recipe"), composterKey = new NamespacedKey(this, "farm_composter_recipe");
        getServer().removeRecipe(cauldronKey); getServer().removeRecipe(composterKey);
        ShapedRecipe cauldron = new ShapedRecipe(cauldronKey, farmItems.createCauldron()); cauldron.shape("IBI", "III"); cauldron.setIngredient('I', Material.IRON_INGOT); cauldron.setIngredient('B', Material.BONE_MEAL); getServer().addRecipe(cauldron);
        ShapedRecipe composter = new ShapedRecipe(composterKey, farmItems.createComposter()); composter.shape("PBP", "PPP"); composter.setIngredient('P', Material.OAK_PLANKS); composter.setIngredient('B', Material.BONE_MEAL); getServer().addRecipe(composter);
    }

    @Override public void onDisable() { if (farmStateManager != null) farmStateManager.save(); if (cropGrowthManager != null) cropGrowthManager.save(); if (treeGrowthManager != null) treeGrowthManager.save(); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("realfarm")) return false;
        if (!sender.hasPermission("realfarm.admin")) { sender.sendMessage("§cУ вас нет прав для этой команды."); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) { sender.sendMessage("§aRealFarm §7v" + getPluginMeta().getVersion()); sender.sendMessage("§7Участок может иметь любую форму, главное — связная пашня без разрывов."); sender.sendMessage("§7Котёл и компостер должны стоять вплотную к пашне."); sender.sendMessage("§7Весна/лето: вода удерживает влажность. Осень: пашня высыхает. Зима: полностью сухая. Дождь увлажняет всегда."); sender.sendMessage("§7Во время дождя отдельно ускоряется природный рост травы и обычных саженцев вне участков RealFarm."); return true; }
        if (args[0].equalsIgnoreCase("status")) { if (!(sender instanceof Player player)) { sender.sendMessage("§cТолько в игре."); return true; } var target = player.getTargetBlockExact(10); if (target == null || target.getType() != Material.FARMLAND) { sender.sendMessage("§eПосмотрите на блок пашни участка."); return true; } FarmStructure farm = validator.findFarmAt(target); if (farm == null) { sender.sendMessage("§cУчасток не найден."); return true; } farmStateManager.refresh(farm); sender.sendMessage("§6Участок №" + farmStateManager.getPlotNumber(farm)); sender.sendMessage("§7Блоков пашни: §f" + farm.farmlandCount()); sender.sendMessage("§7Котёл фермы: " + (farm.hasCauldron() ? "§aесть" : "§cнет")); sender.sendMessage("§7Компостер фермы: " + (farm.hasComposter() ? "§aесть" : "§cнет")); sender.sendMessage("§7Статус воды: " + (farm.isWatered() ? "§aесть" : "§cнет")); return true; }
        if (args[0].equalsIgnoreCase("crops")) { sender.sendMessage("§aКультуры RealFarm:"); cropRegistry.enabled().forEach(crop -> sender.sendMessage("§7- §f" + crop.displayName())); return true; }
        if (args[0].equalsIgnoreCase("reload")) { reloadConfig(); sender.sendMessage("§aКонфигурация перечитана."); return true; }
        sender.sendMessage("§eИспользование: /realfarm <info|status|crops|reload>"); return true;
    }
}
