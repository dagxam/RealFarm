package me.dagxam.realfarm.crop;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Единый реестр культур RealFarm.
 * Сейчас зарегистрированы стандартные культуры Minecraft.
 * Новые культуры будут подключаться через этот же реестр,
 * поэтому ядро роста не придётся переписывать.
 */
public final class CropRegistry {
    private final FileConfiguration config;
    private final Map<Material, CropDefinition> byMaterial = new LinkedHashMap<>();
    private final Map<String, CropDefinition> byId = new LinkedHashMap<>();

    public CropRegistry(FileConfiguration config) {
        this.config = config;
        registerVanillaCrops();
    }

    private void registerVanillaCrops() {
        register("wheat", "Пшеница", Material.WHEAT, "wheat");
        register("carrot", "Морковь", Material.CARROTS, "carrot");
        register("potato", "Картофель", Material.POTATOES, "potato");
        register("beetroot", "Свёкла", Material.BEETROOTS, "beetroot");
        register("torchflower", "Торчфлауэр", Material.TORCHFLOWER_CROP, "torchflower");
        register("pitcher", "Кувшинковое растение", Material.PITCHER_CROP, "pitcher");
    }

    private void register(String id, String displayName, Material material, String configKey) {
        CropDefinition definition = new CropDefinition(id, displayName, material, configKey);
        byMaterial.put(material, definition);
        byId.put(id.toLowerCase(Locale.ROOT), definition);
    }

    public Optional<CropDefinition> get(Material material) {
        CropDefinition definition = byMaterial.get(material);
        if (definition == null || !isEnabled(definition)) return Optional.empty();
        return Optional.of(definition);
    }

    public Optional<CropDefinition> get(String id) {
        if (id == null) return Optional.empty();
        CropDefinition definition = byId.get(id.toLowerCase(Locale.ROOT));
        if (definition == null || !isEnabled(definition)) return Optional.empty();
        return Optional.of(definition);
    }

    public boolean isManaged(Material material) {
        return get(material).isPresent();
    }

    public boolean isEnabled(CropDefinition definition) {
        return config.getBoolean("crops." + definition.configKey() + ".enabled", true);
    }

    public Collection<CropDefinition> all() {
        return byId.values();
    }

    public Collection<CropDefinition> enabled() {
        return byId.values().stream().filter(this::isEnabled).toList();
    }
}
