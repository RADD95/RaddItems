package me.radd.customitems.items;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.util.PdcKeys;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ItemFactory {

    private final RaddItemsPlugin plugin;
    private final CustomItemRegistry registry;

    public ItemFactory(RaddItemsPlugin plugin, CustomItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public ItemStack createItem(String id) {
        CustomItemDefinition def = registry.get(id);
        if (def == null) {
            plugin.debugLog("ItemFactory could not create item: unknown item id '" + id + "'.");
            return null;
        }

        ItemStack stack = new ItemStack(def.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            plugin.debugLog("ItemFactory could not create item '" + def.getId() + "': ItemMeta is null.");
            return stack;
        }

        if (def.getName() != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', def.getName()));
        }

        List<String> lore = def.getLore();
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList());
        }

        if (def.getCustomModelData() > 0) {
            meta.setCustomModelData(def.getCustomModelData());
        }

        meta.setUnbreakable(def.isUnbreakable());

        List<String> itemFlags = def.getItemFlags();
        if (itemFlags != null && !itemFlags.isEmpty()) {
            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase(Locale.ROOT));
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException ignored) {
                    plugin.debugLog("Invalid item flag '" + flagName + "' for item '" + def.getId() + "'.");
                }
            }
        }

        List<CustomItemEnchantmentDefinition> enchantments = def.getEnchantments();
        if (enchantments != null && !enchantments.isEmpty()) {
            for (CustomItemEnchantmentDefinition enchDef : enchantments) {
                if (enchDef == null || enchDef.getType() == null) {
                    plugin.debugLog("Skipped null enchantment definition for item '" + def.getId() + "'.");
                    continue;
                }

                Enchantment enchantment = parseEnchantment(enchDef.getType());
                if (enchantment == null) {
                    plugin.debugLog("Invalid enchantment '" + enchDef.getType() + "' for item '" + def.getId() + "'.");
                    continue;
                }

                meta.addEnchant(enchantment, enchDef.getLevel(), true);
            }
        }

        List<CustomItemAttributeDefinition> attributes = def.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (int i = 0; i < attributes.size(); i++) {
                CustomItemAttributeDefinition attrDef = attributes.get(i);
                if (attrDef == null || attrDef.getAttribute() == null || attrDef.getOperation() == null) {
                    plugin.debugLog("Skipped invalid attribute definition for item '" + def.getId() + "'.");
                    continue;
                }

                Attribute attribute = parseAttribute(attrDef.getAttribute());
                if (attribute == null) {
                    plugin.debugLog("Invalid attribute '" + attrDef.getAttribute() + "' for item '" + def.getId() + "'.");
                    continue;
                }

                AttributeModifier.Operation operation = parseOperation(attrDef.getOperation());
                if (operation == null) {
                    plugin.debugLog("Invalid attribute operation '" + attrDef.getOperation()
                            + "' for item '" + def.getId() + "'.");
                    continue;
                }

                EquipmentSlot nativeSlot = parseNativeAttributeSlot(attrDef.getSlot(), def.getId());

                if (nativeSlot == null) {
                    plugin.debugLog("Logical/non-native attribute slot '" + attrDef.getSlot()
                            + "' detected for item '" + def.getId()
                            + "'. Skipping Bukkit ItemMeta modifier creation here; must be applied manually at runtime.");
                    continue;
                }

                UUID modifierUuid = UUID.nameUUIDFromBytes(
                        (def.getId() + "|" + attribute.name() + "|" + nativeSlot.name() + "|" + i)
                                .getBytes(StandardCharsets.UTF_8)
                );

                AttributeModifier modifier = new AttributeModifier(
                        modifierUuid,
                        "radditems_" + def.getId() + "_" + attribute.name().toLowerCase(Locale.ROOT),
                        attrDef.getAmount(),
                        operation,
                        nativeSlot
                );

                meta.addAttributeModifier(attribute, modifier);
            }
        }

        meta.getPersistentDataContainer().set(
                PdcKeys.CUSTOM_ITEM_ID,
                PersistentDataType.STRING,
                def.getId()
        );

        stack.setItemMeta(meta);
        plugin.debugLog("ItemFactory created item '" + def.getId() + "' successfully.");
        return stack;
    }

    private Enchantment parseEnchantment(String value) {
        try {
            NamespacedKey key;
            if (value.contains(":")) {
                key = NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
            } else {
                key = NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT));
            }

            if (key == null) return null;
            return Registry.ENCHANTMENT.get(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Attribute parseAttribute(String value) {
        try {
            return Attribute.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private AttributeModifier.Operation parseOperation(String value) {
        try {
            return AttributeModifier.Operation.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private EquipmentSlot parseNativeAttributeSlot(String value, String itemId) {
        if (value == null) {
            plugin.debugLog("Missing attribute slot for item '" + itemId + "'.");
            return null;
        }

        String normalized = value.toUpperCase(Locale.ROOT);

        if (normalized.equals("HOTBAR") || normalized.equals("ANY")) {
            return null;
        }

        try {
            return EquipmentSlot.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            plugin.debugLog("Invalid attribute slot '" + value + "' for item '" + itemId + "'.");
            return null;
        }
    }
}