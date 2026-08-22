package me.dagxam.realfarm.crop;

import org.bukkit.Material;

/**
 * Описание культуры, которую контролирует RealFarm.
 * В будущем сюда будут добавлены собственные культуры,
 * использующие стандартные предметы и блоки как временные визуальные основы.
 */
public record CropDefinition(
        String id,
        String displayName,
        Material cropBlock,
        String configKey
) {
}
