package me.radd.customitems.items;

import org.bukkit.Material;

import java.util.List;

public final class CustomItemDefinition {

    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;

    private final int customModelData;
    private final boolean unbreakable;
    private final List<String> itemFlags;

    private final List<CustomItemEnchantmentDefinition> enchantments;
    private final List<CustomItemAttributeDefinition> attributes;

    private final EquipEffectsDefinition equipEffects;
    private final ItemTriggersDefinition triggers;

    public CustomItemDefinition(
            String id,
            Material material,
            String name,
            List<String> lore,
            int customModelData,
            boolean unbreakable,
            List<String> itemFlags,
            List<CustomItemEnchantmentDefinition> enchantments,
            List<CustomItemAttributeDefinition> attributes,
            EquipEffectsDefinition equipEffects,
            ItemTriggersDefinition triggers
    ) {
        this.id = id;
        this.material = material;
        this.name = name;
        this.lore = lore == null ? List.of() : List.copyOf(lore);
        this.customModelData = customModelData;
        this.unbreakable = unbreakable;
        this.itemFlags = itemFlags == null ? List.of() : List.copyOf(itemFlags);
        this.enchantments = enchantments == null ? List.of() : List.copyOf(enchantments);
        this.attributes = attributes == null ? List.of() : List.copyOf(attributes);
        this.equipEffects = equipEffects;
        this.triggers = triggers;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public List<String> getItemFlags() {
        return itemFlags;
    }

    public List<CustomItemEnchantmentDefinition> getEnchantments() {
        return enchantments;
    }

    public List<CustomItemAttributeDefinition> getAttributes() {
        return attributes;
    }

    public EquipEffectsDefinition getEquipEffects() {
        return equipEffects;
    }

    public ItemTriggersDefinition getTriggers() {
        return triggers;
    }
}