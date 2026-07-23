package me.radd.customitems.listeners;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.CustomPotionEffectDefinition;
import me.radd.customitems.items.EquipEffectsDefinition;
import me.radd.customitems.util.CustomItemActivationUtil;
import me.radd.customitems.util.PdcKeys;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EquipEffectsListener implements Listener {

    private static final int CONTINUOUS_EFFECT_DURATION_TICKS = 1200;
    private static final int CONTINUOUS_REFRESH_THRESHOLD_TICKS = 400;
    private static final int IMMEDIATE_DELAY_TICKS = 1;
    private static final int DIRTY_PROCESS_PERIOD_TICKS = 2;
    private static final int BACKUP_CHECK_PERIOD_TICKS = 100;

    private final RaddItemsPlugin plugin;
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> scheduledPlayers = ConcurrentHashMap.newKeySet();

    private BukkitTask dirtyTask;
    private BukkitTask backupTask;

    public EquipEffectsListener(RaddItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        for (Player player : Bukkit.getOnlinePlayers()) {
            markDirty(player);
            scheduleImmediateRefresh(player);
        }

        this.dirtyTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::processDirtyPlayers,
                DIRTY_PROCESS_PERIOD_TICKS,
                DIRTY_PROCESS_PERIOD_TICKS
        );

        this.backupTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::markAllPlayersDirty,
                BACKUP_CHECK_PERIOD_TICKS,
                BACKUP_CHECK_PERIOD_TICKS
        );

        plugin.debugLog("EquipEffectsListener tasks started.");
    }

    public void restart() {
        plugin.debugLog("EquipEffectsListener task restarting.");
        start();
    }

    public void stop() {
        if (dirtyTask != null) {
            dirtyTask.cancel();
            dirtyTask = null;
            plugin.debugLog("EquipEffectsListener dirty task stopped.");
        }

        if (backupTask != null) {
            backupTask.cancel();
            backupTask = null;
            plugin.debugLog("EquipEffectsListener backup task stopped.");
        }

        dirtyPlayers.clear();
        scheduledPlayers.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        markDirty(event.getPlayer());
        scheduleImmediateRefresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        dirtyPlayers.remove(uuid);
        scheduledPlayers.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        markDirty(event.getPlayer());
        scheduleImmediateRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        markDirty(event.getPlayer());
        scheduleImmediateRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        markDirty(event.getPlayer());
        scheduleImmediateRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            markDirty(player);
            Bukkit.getScheduler().runTask(plugin, () -> markDirty(player));
            scheduleImmediateRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                markDirty(player);
                scheduleImmediateRefresh(player);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            markDirty(player);
            scheduleImmediateRefresh(player);
        }
    }

    private void markDirty(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        dirtyPlayers.add(player.getUniqueId());
    }

    private void markAllPlayersDirty() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            markDirty(player);
        }
    }

    private void scheduleImmediateRefresh(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!scheduledPlayers.add(uuid)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) {
                    markDirty(online);
                }
            } finally {
                scheduledPlayers.remove(uuid);
            }
        }, IMMEDIATE_DELAY_TICKS);
    }

    private void processDirtyPlayers() {
        if (dirtyPlayers.isEmpty()) {
            return;
        }

        List<UUID> toProcess = new ArrayList<>(dirtyPlayers);
        dirtyPlayers.clear();

        for (UUID uuid : toProcess) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            applyEquipEffects(player);
        }
    }

    private void applyEquipEffects(Player player) {
        processSlotItem(player, player.getInventory().getHelmet(), "HEAD");
        processSlotItem(player, player.getInventory().getChestplate(), "CHEST");
        processSlotItem(player, player.getInventory().getLeggings(), "LEGS");
        processSlotItem(player, player.getInventory().getBoots(), "FEET");
        processSlotItem(player, player.getInventory().getItemInMainHand(), "HAND");
        processSlotItem(player, player.getInventory().getItemInOffHand(), "OFF_HAND");

        for (int i = 0; i <= 8; i++) {
            processSlotItem(player, player.getInventory().getItem(i), "HOTBAR");
        }
    }

    private void processSlotItem(Player player, ItemStack item, String logicalSlot) {
        String itemId = getCustomItemId(item);
        if (itemId == null) {
            return;
        }

        CustomItemDefinition def = plugin.getItemRegistry().get(itemId);
        if (def == null) {
            plugin.debugLog("Equip effects skipped: item '" + itemId + "' not found in registry.");
            return;
        }

        EquipEffectsDefinition equipEffects = def.getEquipEffects();
        if (equipEffects == null || !equipEffects.isEnabled()) {
            return;
        }

        List<String> activeSlots = equipEffects.getActiveSlots();
        if (activeSlots == null || activeSlots.isEmpty()) {
            return;
        }

        if (!containsLogicalSlot(activeSlots, logicalSlot)) {
            return;
        }

        if (!CustomItemActivationUtil.isItemActiveForEquipEffects(player, def)) {
            return;
        }

        List<CustomPotionEffectDefinition> potionEffects = equipEffects.getPotionEffects();
        if (potionEffects == null || potionEffects.isEmpty()) {
            return;
        }

        for (CustomPotionEffectDefinition effectDef : potionEffects) {
            if (effectDef == null || effectDef.getType() == null) {
                continue;
            }

            PotionEffectType type = parsePotionEffectType(effectDef.getType());
            if (type == null) {
                plugin.debugLog("Invalid equip effect type '" + effectDef.getType()
                        + "' for item '" + itemId + "'.");
                continue;
            }

            PotionEffect current = player.getPotionEffect(type);
            int desiredDuration = CONTINUOUS_EFFECT_DURATION_TICKS;
            int desiredAmplifier = effectDef.getAmplifier();

            boolean needsRefresh = current == null
                    || current.getAmplifier() != desiredAmplifier
                    || current.getDuration() < CONTINUOUS_REFRESH_THRESHOLD_TICKS;

            if (!needsRefresh) {
                continue;
            }

            player.addPotionEffect(new PotionEffect(
                    type,
                    desiredDuration,
                    desiredAmplifier,
                    effectDef.isAmbient(),
                    effectDef.hasParticles(),
                    effectDef.hasIcon()
            ));

            plugin.debugLog("Applied equip effect '" + type.getKey() + "' to player '"
                    + player.getName() + "' from item '" + itemId + "' in slot '" + logicalSlot
                    + "' with fixed duration " + desiredDuration + " ticks.");
        }
    }

    private boolean containsLogicalSlot(List<String> activeSlots, String expectedSlot) {
        for (String slot : activeSlots) {
            if (slot != null && slot.equalsIgnoreCase(expectedSlot)) {
                return true;
            }
        }
        return false;
    }

    private String getCustomItemId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        return item.getItemMeta().getPersistentDataContainer().get(
                PdcKeys.CUSTOM_ITEM_ID,
                PersistentDataType.STRING
        );
    }

    private PotionEffectType parsePotionEffectType(String value) {
        try {
            NamespacedKey key;
            if (value.contains(":")) {
                key = NamespacedKey.fromString(value.toLowerCase(Locale.ROOT));
            } else {
                key = NamespacedKey.minecraft(value.toLowerCase(Locale.ROOT));
            }

            if (key == null) {
                return null;
            }

            return Registry.EFFECT.get(key);
        } catch (Exception ignored) {
            return null;
        }
    }
}