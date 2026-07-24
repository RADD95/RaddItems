package me.radd.customitems.util;

import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.EquipEffectsDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
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

        if (normalized.equals("ANY")) {
            return resolveFirstMatchingAnySlot(inv, equipment, expectedItemId) != null;
        }

        return matchesConcreteSlot(inv, equipment, expectedItemId, normalized);
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
            if (resolved.isBlank()) {
                continue;
            }

            if (resolved.equals("ANY")) {
                normalized.addAll(ANY_SLOT_ORDER);
            } else {
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

    public static List<String> resolveAllActiveSlots(Player player, CustomItemDefinition def) {
        if (player == null || def == null || def.getId() == null || def.getId().isBlank()) {
            return Collections.emptyList();
        }

        return findAllMatchingSlots(player, def.getId(), resolveConfiguredActiveSlots(def));
    }

    public static List<String> resolveAllActiveSlotsForEquipEffects(Player player, CustomItemDefinition def) {
        if (player == null || def == null || def.getId() == null || def.getId().isBlank()) {
            return Collections.emptyList();
        }

        return findAllMatchingSlots(player, def.getId(), resolveEquipEffectActiveSlots(def));
    }

    private static String findFirstMatchingSlot(Player player, String expectedItemId, List<String> requestedSlots) {
        List<String> matches = findAllMatchingSlots(player, expectedItemId, requestedSlots);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static List<String> findAllMatchingSlots(Player player, String expectedItemId, List<String> requestedSlots) {
        if (requestedSlots == null || requestedSlots.isEmpty()) {
            return Collections.emptyList();
        }

        PlayerInventory inv = player.getInventory();
        EntityEquipment equipment = player.getEquipment();
        List<String> matches = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();

        for (String slot : requestedSlots) {
            if (slot == null || slot.isBlank()) {
                continue;
            }

            String normalized = normalizeSlot(slot);
            if (normalized.isBlank()) {
                continue;
            }

            if (normalized.equals("ANY")) {
                String resolvedAny = resolveFirstMatchingAnySlot(inv, equipment, expectedItemId);
                if (resolvedAny != null && added.add(resolvedAny)) {
                    matches.add(resolvedAny);
                }
                continue;
            }

            if (matchesConcreteSlot(inv, equipment, expectedItemId, normalized) && added.add(normalized)) {
                matches.add(normalized);
            }
        }

        return matches;
    }

    private static String resolveFirstMatchingAnySlot(PlayerInventory inv, EntityEquipment equipment, String expectedItemId) {
        for (String slot : ANY_SLOT_ORDER) {
            if (matchesConcreteSlot(inv, equipment, expectedItemId, slot)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean matchesConcreteSlot(PlayerInventory inv, EntityEquipment equipment, String expectedItemId, String normalizedSlot) {
        return switch (normalizedSlot) {
            case "HAND" -> matchesCustomItem(inv.getItemInMainHand(), expectedItemId);
            case "OFF_HAND" -> matchesCustomItem(inv.getItemInOffHand(), expectedItemId);
            case "HOTBAR" -> hotbarContains(inv, expectedItemId);
            case "HEAD" -> equipment != null && matchesCustomItem(equipment.getHelmet(), expectedItemId);
            case "CHEST" -> equipment != null && matchesCustomItem(equipment.getChestplate(), expectedItemId);
            case "LEGS" -> equipment != null && matchesCustomItem(equipment.getLeggings(), expectedItemId);
            case "FEET" -> equipment != null && matchesCustomItem(equipment.getBoots(), expectedItemId);
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