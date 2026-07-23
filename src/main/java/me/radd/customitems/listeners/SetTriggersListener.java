package me.radd.customitems.listeners;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.CustomPotionEffectDefinition;
import me.radd.customitems.items.TriggerEffectDefinition;
import me.radd.customitems.sets.ItemSetDefinition;
import me.radd.customitems.util.CustomItemActivationUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SetTriggersListener implements Listener {

    private static final int IMMEDIATE_DELAY_TICKS = 1;
    private static final int DIRTY_PROCESS_PERIOD_TICKS = 2;
    private static final int BACKUP_CHECK_PERIOD_TICKS = 100;

    private final RaddItemsPlugin plugin;
    private final Map<UUID, Set<String>> lastKnownActiveSets = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> scheduledPlayers = ConcurrentHashMap.newKeySet();

    private BukkitTask dirtyTask;
    private BukkitTask backupTask;

    public SetTriggersListener(RaddItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        for (Player player : Bukkit.getOnlinePlayers()) {
            lastKnownActiveSets.put(player.getUniqueId(), Collections.emptySet());
            markDirty(player);
            scheduleImmediateCheck(player);
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

        plugin.debugLog("SetTriggersListener tasks started.");
    }

    public void restart() {
        plugin.debugLog("SetTriggersListener task restarting.");
        start();
    }

    public void stop() {
        if (dirtyTask != null) {
            dirtyTask.cancel();
            dirtyTask = null;
            plugin.debugLog("SetTriggersListener dirty task stopped.");
        }

        if (backupTask != null) {
            backupTask.cancel();
            backupTask = null;
            plugin.debugLog("SetTriggersListener backup task stopped.");
        }

        lastKnownActiveSets.clear();
        dirtyPlayers.clear();
        scheduledPlayers.clear();
        plugin.debugLog("SetTriggersListener cached active sets cleared.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lastKnownActiveSets.put(player.getUniqueId(), Collections.emptySet());
        markDirty(player);
        scheduleImmediateCheck(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastKnownActiveSets.remove(uuid);
        dirtyPlayers.remove(uuid);
        scheduledPlayers.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        markDirty(event.getPlayer());
        scheduleImmediateCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        markDirty(event.getPlayer());
        scheduleImmediateCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        markDirty(event.getPlayer());
        scheduleImmediateCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            markDirty(player);
            Bukkit.getScheduler().runTask(plugin, () -> markDirty(player));
            scheduleImmediateCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                markDirty(player);
                scheduleImmediateCheck(player);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            markDirty(player);
            scheduleImmediateCheck(player);
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

    private void scheduleImmediateCheck(Player player) {
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

            checkPlayer(player);
        }
    }

    private void checkPlayer(Player player) {
        Set<String> previous = lastKnownActiveSets.getOrDefault(player.getUniqueId(), Collections.emptySet());
        Set<String> current = captureActiveSets(player);

        for (String setId : previous) {
            if (!current.contains(setId)) {
                fireDeactivate(player, setId);
            }
        }

        for (String setId : current) {
            if (!previous.contains(setId)) {
                fireActivate(player, setId);
            }
        }

        lastKnownActiveSets.put(player.getUniqueId(), current);
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

    private void captureActivePiece(Player player, ItemStack item, Set<String> activePieceIds) {
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

    private void fireActivate(Player player, String setId) {
        ItemSetDefinition set = plugin.getSetRegistry().get(setId);
        if (set == null) {
            plugin.debugLog("on_activate skipped: set '" + setId + "' not found in registry.");
            return;
        }

        TriggerEffectDefinition trigger = set.getOnActivate();
        if (trigger == null || !trigger.isEnabled()) {
            return;
        }

        applySelfEffects(player, trigger, "on_activate", setId);
        executeCommands(player, setId, trigger);
    }

    private void fireDeactivate(Player player, String setId) {
        ItemSetDefinition set = plugin.getSetRegistry().get(setId);
        if (set == null) {
            plugin.debugLog("on_deactivate skipped: set '" + setId + "' not found in registry.");
            return;
        }

        TriggerEffectDefinition trigger = set.getOnDeactivate();
        if (trigger == null || !trigger.isEnabled()) {
            return;
        }

        applySelfEffects(player, trigger, "on_deactivate", setId);
        executeCommands(player, setId, trigger);
    }

    private void applySelfEffects(Player player, TriggerEffectDefinition trigger, String triggerName, String setId) {
        List<CustomPotionEffectDefinition> effects = trigger.getPotionEffectsSelf();
        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (CustomPotionEffectDefinition effectDef : effects) {
            if (effectDef == null || effectDef.getType() == null) {
                continue;
            }

            PotionEffectType type = parsePotionEffectType(effectDef.getType());
            if (type == null) {
                plugin.debugLog("Invalid self effect type '" + effectDef.getType()
                        + "' in " + triggerName + " for set '" + setId + "'.");
                continue;
            }

            player.addPotionEffect(new PotionEffect(
                    type,
                    effectDef.getDuration(),
                    effectDef.getAmplifier(),
                    effectDef.isAmbient(),
                    effectDef.hasParticles(),
                    effectDef.hasIcon()
            ));
        }
    }

    private void executeCommands(Player player, String setId, TriggerEffectDefinition trigger) {
        List<String> commands = trigger.getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }

        CommandSender console = Bukkit.getServer().getConsoleSender();

        for (String command : commands) {
            String parsed = command
                    .replace("%player%", player.getName())
                    .replace("%set_id%", setId);

            Bukkit.dispatchCommand(console, parsed);
        }
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