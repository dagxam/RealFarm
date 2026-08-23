package me.dagxam.realfarm.farm;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class FarmItems {
    public static final String TYPE_CAULDRON = "cauldron";
    public static final String TYPE_COMPOSTER = "composter";
    private final NamespacedKey key;

    public FarmItems(JavaPlugin plugin) { this.key = new NamespacedKey(plugin, "farm_service"); }

    public ItemStack createCauldron() { return create(Material.CAULDRON, "Котёл фермы", TYPE_CAULDRON); }
    public ItemStack createComposter() { return create(Material.COMPOSTER, "Компостер фермы", TYPE_COMPOSTER); }

    private ItemStack create(Material material, String name, String type) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCauldron(ItemStack item) { return hasType(item, TYPE_CAULDRON); }
    public boolean isComposter(ItemStack item) { return hasType(item, TYPE_COMPOSTER); }

    private boolean hasType(ItemStack item, String type) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        return type.equals(item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }
}
