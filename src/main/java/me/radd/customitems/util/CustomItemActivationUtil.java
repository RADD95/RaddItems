package me.radd.customitems.util;

import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.EquipEffectsDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CustomItemActivationUtil {

    private static final List<String> ANY_SLOT_ORDER = List.of(
            "HAND", "OFF_HAND", "HOTBAR", "HEAD", "CHEST", "LEGS", "FEET"
    );

    private CustomItemActivationUtil() {
    }

    public static String getCustomItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(PdcKeys.CUSTOM_ITEM_ID, PersistentDataType.STRING);
    }

    public static boolean matchesCustomItem(ItemStack item, String expectedId) {
        if (expectedId == null || expectedId.isBlank()) {
            return false;
        }

        return expectedId.equals(getCustomItemId(item));
    }

    public static boolean isInHotbar(Player player, String expectedItemId) {
        if (player == null || expectedItemId == null || expectedItemId.isBlank()) {
            return false;
        }

        return hotbarContains(player.getInventory(), expectedItemId);
    }

    public static boolean isItemInLogicalSlot(Player player, String expectedItemId, String slotName) {
        if (player == null || expectedItemId == null || expectedItemId.isBlank() || slotName == null || slotName.isBlank()) {
            return false;
        }

        String normalized = normalizeSlot(slotName);
        if (normalized.isBlank()) {
            return false;
        }

        PlayerInventory inv = player.getInventory();
        EntityEquipment equipment = player.getEquipment();
        return matchesSlot(inv, equipment, expectedItemId, normalized);
    }

    public static List<String> resolveConfiguredActiveSlots(CustomItemDefinition def) {
        if (def == null) {
            return Collections.emptyList();
        }

        EquipEffectsDefinition equipEffects = def.getEquipEffects();
        if (equipEffects == null) {
            return Collections.emptyList();
        }

        List<String> rawSlots = equipEffects.getActiveSlots();
        if (rawSlots == null || rawSlots.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String slot : rawSlots) {
            String resolved = normalizeSlot(slot);
            if (!resolved.isBlank()) {
                normalized.add(resolved);
            }
        }

        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        return List.copyOf(normalized);
    }

    public static List<String> resolveEquipEffectActiveSlots(CustomItemDefinition def) {
        if (def == null) {
            return Collections.emptyList();
        }

        EquipEffectsDefinition equipEffects = def.getEquipEffects();
        if (equipEffects == null || !equipEffects.isEnabled()) {
            return Collections.emptyList();
        }

        return resolveConfiguredActiveSlots(def);
    }

    public static boolean isItemActive(Player player, CustomItemDefinition def) {
        if (player == null || def == null || def.getId() == null || def.getId().isBlank()) {
            return false;
        }

        return findFirstMatchingSlot(player, def.getId(), resolveConfiguredActiveSlots(def)) != null;
    }

    public static boolean isItemActiveForEquipEffects(Player player, CustomItemDefinition def) {
        if (player == null || def == null || def.getId() == null || def.getId().isBlank()) {
            return false;
        }

        return findFirstMatchingSlot(player, def.getId(), resolveEquipEffectActiveSlots(def)) != null;
    }

    public static String resolveFirstActiveSlot(Player player, CustomItemDefinition def) {
        if (player == null || def == null || def.getId() == null || def.getId().isBlank()) {
            return "UNKNOWN";
        }

        String slot = findFirstMatchingSlot(player, def.getId(), resolveConfiguredActiveSlots(def));
        return slot != null ? slot : "UNKNOWN";
    }

    public static String resolveFirstActiveSlotForEquipEffects(Player player, CustomItemDefinition def) {
        if (player == null || def == null || def.getId() == null || def.getId().isBlank()) {
            return "UNKNOWN";
        }

        String slot = findFirstMatchingSlot(player, def.getId(), resolveEquipEffectActiveSlots(def));
        return slot != null ? slot : "UNKNOWN";
    }

    private static String findFirstMatchingSlot(Player player, String expectedItemId, List<String> requestedSlots) {
        if (requestedSlots == null || requestedSlots.isEmpty()) {
            return null;
        }

        PlayerInventory inv = player.getInventory();
        EntityEquipment equipment = player.getEquipment();

        for (String slot : requestedSlots) {
            if (slot == null || slot.isBlank()) {
                continue;
            }

            if (matchesSlot(inv, equipment, expectedItemId, slot)) {
                return slot;
            }
        }

        return null;
    }

    private static boolean matchesSlot(PlayerInventory inv, EntityEquipment equipment, String expectedItemId, String normalizedSlot) {
        return switch (normalizedSlot) {
            case "HAND" -> matchesCustomItem(inv.getItemInMainHand(), expectedItemId);
            case "OFF_HAND" -> matchesCustomItem(inv.getItemInOffHand(), expectedItemId);
            case "HOTBAR" -> hotbarContains(inv, expectedItemId);
            case "HEAD" -> equipment != null && matchesCustomItem(equipment.getHelmet(), expectedItemId);
            case "CHEST" -> equipment != null && matchesCustomItem(equipment.getChestplate(), expectedItemId);
            case "LEGS" -> equipment != null && matchesCustomItem(equipment.getLeggings(), expectedItemId);
            case "FEET" -> equipment != null && matchesCustomItem(equipment.getBoots(), expectedItemId);
            case "ANY" -> matchesAny(inv, equipment, expectedItemId);
            default -> false;
        };
    }

    private static boolean hotbarContains(PlayerInventory inv, String expectedItemId) {
        for (int i = 0; i < 9; i++) {
            if (matchesCustomItem(inv.getItem(i), expectedItemId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAny(PlayerInventory inv, EntityEquipment equipment, String expectedItemId) {
        if (matchesCustomItem(inv.getItemInMainHand(), expectedItemId)) {
            return true;
        }
        if (matchesCustomItem(inv.getItemInOffHand(), expectedItemId)) {
            return true;
        }
        if (hotbarContains(inv, expectedItemId)) {
            return true;
        }
        if (equipment != null && matchesCustomItem(equipment.getHelmet(), expectedItemId)) {
            return true;
        }
        if (equipment != null && matchesCustomItem(equipment.getChestplate(), expectedItemId)) {
            return true;
        }
        if (equipment != null && matchesCustomItem(equipment.getLeggings(), expectedItemId)) {
            return true;
        }
        return equipment != null && matchesCustomItem(equipment.getBoots(), expectedItemId);
    }

    private static String normalizeSlot(String slot) {
        if (slot == null || slot.isBlank()) {
            return "";
        }

        String normalized = slot.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MAIN_HAND" -> "HAND";
            case "OFFHAND" -> "OFF_HAND";
            case "HELMET" -> "HEAD";
            case "CHESTPLATE" -> "CHEST";
            case "LEGGINGS" -> "LEGS";
            case "BOOTS" -> "FEET";
            default -> normalized;
        };
    }
}