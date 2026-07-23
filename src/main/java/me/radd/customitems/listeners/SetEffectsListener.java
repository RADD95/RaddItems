package me.radd.customitems.listeners;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.items.CustomItemAttributeDefinition;
import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.CustomPotionEffectDefinition;
import me.radd.customitems.sets.ItemSetDefinition;
import me.radd.customitems.util.CustomItemActivationUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SetEffectsListener implements Listener {

    private static final int CONTINUOUS_EFFECT_DURATION_TICKS = 1200;
    private static final int CONTINUOUS_REFRESH_THRESHOLD_TICKS = 400;
    private static final int IMMEDIATE_DELAY_TICKS = 1;
    private static final int DIRTY_PROCESS_PERIOD_TICKS = 2;
    private static final int BACKUP_CHECK_PERIOD_TICKS = 100;

    private final RaddItemsPlugin plugin;
    private final Map<UUID, Set<String>> activeSetsByPlayer = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> scheduledPlayers = ConcurrentHashMap.newKeySet();

    private BukkitTask dirtyTask;
    private BukkitTask backupTask;

    public SetEffectsListener(RaddItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        for (Player player : Bukkit.getOnlinePlayers()) {
            activeSetsByPlayer.put(player.getUniqueId(), Collections.emptySet());
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

        plugin.debugLog("SetEffectsListener tasks started.");
    }

    public void restart() {
        plugin.debugLog("SetEffectsListener task restarting.");
        start();
    }

    public void stop() {
        if (dirtyTask != null) {
            dirtyTask.cancel();
            dirtyTask = null;
            plugin.debugLog("SetEffectsListener dirty task stopped.");
        }

        if (backupTask != null) {
            backupTask.cancel();
            backupTask = null;
            plugin.debugLog("SetEffectsListener backup task stopped.");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            clearAllSetAttributes(player);
        }

        activeSetsByPlayer.clear();
        dirtyPlayers.clear();
        scheduledPlayers.clear();
        plugin.debugLog("All set attributes cleared from online players.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        activeSetsByPlayer.put(player.getUniqueId(), Collections.emptySet());
        markDirty(player);
        scheduleImmediateRefresh(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeSetsByPlayer.remove(uuid);
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

            updatePlayerSets(player);
        }
    }

    private void updatePlayerSets(Player player) {
        Set<String> previous = activeSetsByPlayer.getOrDefault(player.getUniqueId(), Collections.emptySet());
        Set<String> current = captureActiveSets(player);

        for (String setId : current) {
            ItemSetDefinition set = plugin.getSetRegistry().get(setId);
            if (set == null) {
                continue;
            }

            if (!previous.contains(setId)) {
                applyAttributes(player, set);
                plugin.debugLog("Activated set '" + setId + "' for player '" + player.getName() + "'.");
            }

            applyPotionEffects(player, set);
        }

        for (String setId : previous) {
            if (!current.contains(setId)) {
                ItemSetDefinition set = plugin.getSetRegistry().get(setId);
                if (set != null) {
                    removeAttributes(player, set);
                    plugin.debugLog("Deactivated set '" + setId + "' for player '" + player.getName() + "'.");
                }
            }
        }

        activeSetsByPlayer.put(player.getUniqueId(), current);
    }

    private Set<String> captureActiveSets(Player player) {
        Set<String> activePieceIds = captureActivePieceIds(player);
        if (activePieceIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> candidateSetIds = new HashSet<>();
        for (String pieceId : activePieceIds) {
            candidateSetIds.addAll(plugin.getSetRegistry().getCandidateSetIdsForPiece(pieceId));
        }

        if (candidateSetIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> activeSets = new HashSet<>();
        for (String setId : candidateSetIds) {
            ItemSetDefinition set = plugin.getSetRegistry().get(setId);
            if (set == null) {
                continue;
            }

            int matched = countMatchedPieces(activePieceIds, set);
            if (matched >= set.getRequiredAmount()) {
                activeSets.add(setId);
            }
        }

        return activeSets;
    }

    private Set<String> captureActivePieceIds(Player player) {
        Set<String> activePieceIds = new HashSet<>();

        captureActivePiece(player, player.getInventory().getHelmet(), activePieceIds);
        captureActivePiece(player, player.getInventory().getChestplate(), activePieceIds);
        captureActivePiece(player, player.getInventory().getLeggings(), activePieceIds);
        captureActivePiece(player, player.getInventory().getBoots(), activePieceIds);
        captureActivePiece(player, player.getInventory().getItemInMainHand(), activePieceIds);
        captureActivePiece(player, player.getInventory().getItemInOffHand(), activePieceIds);

        for (int i = 0; i <= 8; i++) {
            captureActivePiece(player, player.getInventory().getItem(i), activePieceIds);
        }

        return activePieceIds;
    }

    private void captureActivePiece(Player player, org.bukkit.inventory.ItemStack item, Set<String> activePieceIds) {
        String itemId = CustomItemActivationUtil.getCustomItemId(item);
        if (itemId == null) {
            return;
        }

        CustomItemDefinition def = plugin.getItemRegistry().get(itemId);
        if (def == null) {
            return;
        }

        if (CustomItemActivationUtil.isItemActive(player, def)) {
            activePieceIds.add(itemId);
        }
    }

    private void applyPotionEffects(Player player, ItemSetDefinition set) {
        List<CustomPotionEffectDefinition> effects = set.getPotionEffects();
        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (CustomPotionEffectDefinition effectDef : effects) {
            if (effectDef == null || effectDef.getType() == null) {
                continue;
            }

            PotionEffectType type = parsePotionEffectType(effectDef.getType());
            if (type == null) {
                plugin.debugLog("Invalid set potion effect type '" + effectDef.getType()
                        + "' in set '" + set.getId() + "'.");
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

            plugin.debugLog("Applied set effect '" + type.getKey() + "' to player '"
                    + player.getName() + "' from set '" + set.getId()
                    + "' with fixed duration " + desiredDuration + " ticks.");
        }
    }

    private void applyAttributes(Player player, ItemSetDefinition set) {
        if (set.getAttributes() == null || set.getAttributes().isEmpty()) {
            return;
        }

        for (CustomItemAttributeDefinition attrDef : set.getAttributes()) {
            if (attrDef == null || attrDef.getAttribute() == null || attrDef.getOperation() == null) {
                continue;
            }

            Attribute attribute = parseAttribute(attrDef.getAttribute());
            if (attribute == null) {
                plugin.debugLog("Invalid set attribute '" + attrDef.getAttribute()
                        + "' in set '" + set.getId() + "'.");
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            AttributeModifier.Operation operation = parseOperation(attrDef.getOperation());
            if (operation == null) {
                plugin.debugLog("Invalid set attribute operation '" + attrDef.getOperation()
                        + "' in set '" + set.getId() + "'.");
                continue;
            }

            UUID uuid = buildModifierUuid(set.getId(), attrDef);
            removeModifier(instance, uuid);

            AttributeModifier modifier = new AttributeModifier(
                    uuid,
                    "radditems_set_" + set.getId(),
                    attrDef.getAmount(),
                    operation
            );

            instance.addModifier(modifier);
        }
    }

    private void removeAttributes(Player player, ItemSetDefinition set) {
        if (set.getAttributes() == null || set.getAttributes().isEmpty()) {
            return;
        }

        for (CustomItemAttributeDefinition attrDef : set.getAttributes()) {
            if (attrDef == null || attrDef.getAttribute() == null) {
                continue;
            }

            Attribute attribute = parseAttribute(attrDef.getAttribute());
            if (attribute == null) {
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            UUID uuid = buildModifierUuid(set.getId(), attrDef);
            removeModifier(instance, uuid);
        }
    }

    private void clearAllSetAttributes(Player player) {
        for (String setId : plugin.getSetRegistry().getIds()) {
            ItemSetDefinition set = plugin.getSetRegistry().get(setId);
            if (set != null) {
                removeAttributes(player, set);
            }
        }
    }

    private void removeModifier(AttributeInstance instance, UUID uuid) {
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getUniqueId().equals(uuid)) {
                instance.removeModifier(modifier);
                break;
            }
        }
    }

    private UUID buildModifierUuid(String setId, CustomItemAttributeDefinition attrDef) {
        String raw = setId + ":" + attrDef.getAttribute() + ":" + attrDef.getOperation();
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }

    private int countMatchedPieces(Set<String> activePieceIds, ItemSetDefinition set) {
        List<String> pieces = set.getPieces();
        if (pieces == null || pieces.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String pieceId : pieces) {
            if (pieceId != null && activePieceIds.contains(pieceId)) {
                count++;
            }
        }

        return count;
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

    private Attribute parseAttribute(String value) {
        try {
            return Attribute.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private AttributeModifier.Operation parseOperation(String value) {
        try {
            return AttributeModifier.Operation.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }
}