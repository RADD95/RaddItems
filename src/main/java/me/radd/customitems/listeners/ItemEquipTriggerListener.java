package me.radd.customitems.listeners;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.CustomPotionEffectDefinition;
import me.radd.customitems.items.EquipEffectsDefinition;
import me.radd.customitems.items.ItemTriggersDefinition;
import me.radd.customitems.items.TriggerEffectDefinition;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemEquipTriggerListener implements Listener {

    private static final int IMMEDIATE_DELAY_TICKS = 1;
    private static final int BACKUP_CHECK_PERIOD_TICKS = 20;

    private final RaddItemsPlugin plugin;
    private final Map<UUID, Set<String>> lastKnownActiveItems = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> lastKnownActiveSlots = new ConcurrentHashMap<>();
    private final Set<UUID> scheduledPlayers = ConcurrentHashMap.newKeySet();
    private BukkitTask backupTask;

    public ItemEquipTriggerListener(RaddItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        for (Player player : Bukkit.getOnlinePlayers()) {
            lastKnownActiveItems.put(player.getUniqueId(), captureActiveItems(player));
            lastKnownActiveSlots.put(player.getUniqueId(), captureActiveItemSlots(player));
            scheduleImmediateCheck(player);
        }

        this.backupTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::checkAllOnlinePlayers,
                BACKUP_CHECK_PERIOD_TICKS,
                BACKUP_CHECK_PERIOD_TICKS
        );

        plugin.debugLog("ItemEquipTriggerListener task started.");
    }

    public void restart() {
        plugin.debugLog("ItemEquipTriggerListener task restarting.");
        start();
    }

    public void stop() {
        if (backupTask != null) {
            backupTask.cancel();
            backupTask = null;
            plugin.debugLog("ItemEquipTriggerListener task stopped.");
        }

        lastKnownActiveItems.clear();
        lastKnownActiveSlots.clear();
        scheduledPlayers.clear();
        plugin.debugLog("ItemEquipTriggerListener cached active items cleared.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lastKnownActiveItems.put(player.getUniqueId(), captureActiveItems(player));
        lastKnownActiveSlots.put(player.getUniqueId(), captureActiveItemSlots(player));
        scheduleImmediateCheck(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastKnownActiveItems.remove(uuid);
        lastKnownActiveSlots.remove(uuid);
        scheduledPlayers.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        scheduleImmediateCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        scheduleImmediateCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        scheduleImmediateCheck(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleImmediateCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleImmediateCheck(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            scheduleImmediateCheck(player);
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
                    checkPlayer(online);
                }
            } finally {
                scheduledPlayers.remove(uuid);
            }
        }, IMMEDIATE_DELAY_TICKS);
    }

    private void checkAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayer(player);
        }
    }

    private void checkPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        Set<String> previous = lastKnownActiveItems.get(uuid);
        if (previous == null) {
            previous = new HashSet<>();
        }

        Map<String, String> previousSlots = lastKnownActiveSlots.get(uuid);
        if (previousSlots == null) {
            previousSlots = new HashMap<>();
        }

        Set<String> current = captureActiveItems(player);
        Map<String, String> currentSlots = captureActiveItemSlots(player);

        for (String itemId : previous) {
            if (!current.contains(itemId)) {
                String previousSlot = previousSlots.getOrDefault(itemId, "UNKNOWN");
                fireUnequip(player, itemId, previousSlot);
            }
        }

        for (String itemId : current) {
            if (!previous.contains(itemId)) {
                String currentSlot = currentSlots.getOrDefault(itemId, "UNKNOWN");
                fireEquip(player, itemId, currentSlot);
            }
        }

        lastKnownActiveItems.put(uuid, current);
        lastKnownActiveSlots.put(uuid, currentSlots);
    }

    private void fireEquip(Player player, String itemId, String slot) {
        CustomItemDefinition def = plugin.getItemRegistry().get(itemId);
        if (def == null) {
            plugin.debugLog("on_equip skipped: item '" + itemId + "' not found in registry.");
            return;
        }

        ItemTriggersDefinition triggers = def.getTriggers();
        if (triggers == null) {
            return;
        }

        TriggerEffectDefinition trigger = triggers.getOnEquip();
        if (trigger == null || !trigger.isEnabled()) {
            return;
        }

        applySelfEffects(player, trigger, "on_equip", itemId);
        executeCommands(player, itemId, trigger, slot);
    }

    private void fireUnequip(Player player, String itemId, String slot) {
        CustomItemDefinition def = plugin.getItemRegistry().get(itemId);
        if (def == null) {
            plugin.debugLog("on_unequip skipped: item '" + itemId + "' not found in registry.");
            return;
        }

        ItemTriggersDefinition triggers = def.getTriggers();
        if (triggers == null) {
            return;
        }

        TriggerEffectDefinition trigger = triggers.getOnUnequip();
        if (trigger == null || !trigger.isEnabled()) {
            return;
        }

        applySelfEffects(player, trigger, "on_unequip", itemId);
        executeCommands(player, itemId, trigger, slot);
    }

    private void applySelfEffects(Player player, TriggerEffectDefinition trigger, String triggerName, String itemId) {
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
                        + "' in " + triggerName + " for item '" + itemId + "'.");
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

    private Set<String> captureActiveItems(Player player) {
        return captureActiveItemSlots(player).keySet();
    }

    private Map<String, String> captureActiveItemSlots(Player player) {
        Map<String, String> active = new HashMap<>();

        captureIfActive(player, player.getInventory().getHelmet(), "HEAD", active);
        captureIfActive(player, player.getInventory().getChestplate(), "CHEST", active);
        captureIfActive(player, player.getInventory().getLeggings(), "LEGS", active);
        captureIfActive(player, player.getInventory().getBoots(), "FEET", active);
        captureIfActive(player, player.getInventory().getItemInMainHand(), "HAND", active);
        captureIfActive(player, player.getInventory().getItemInOffHand(), "OFF_HAND", active);

        for (int i = 0; i <= 8; i++) {
            captureIfActive(player, player.getInventory().getItem(i), "HOTBAR", active);
        }

        return active;
    }

    private void captureIfActive(Player player, org.bukkit.inventory.ItemStack item, String logicalSlot, Map<String, String> active) {
        String itemId = CustomItemActivationUtil.getCustomItemId(item);
        if (itemId == null) {
            return;
        }

        CustomItemDefinition def = plugin.getItemRegistry().get(itemId);
        if (def == null) {
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

        boolean slotMatches = false;
        for (String slot : activeSlots) {
            if (slot == null) {
                continue;
            }

            if (slot.equalsIgnoreCase("ANY") || slot.equalsIgnoreCase(logicalSlot)) {
                slotMatches = true;
                break;
            }
        }

        if (!slotMatches) {
            return;
        }

        if (CustomItemActivationUtil.isItemActiveForEquipEffects(player, def)) {
            active.put(itemId, logicalSlot);
        }
    }

    private void executeCommands(Player player, String itemId, TriggerEffectDefinition trigger, String slotName) {
        List<String> commands = trigger.getCommands();
        if (commands == null || commands.isEmpty()) {
            return;
        }

        CommandSender console = Bukkit.getServer().getConsoleSender();

        for (String command : commands) {
            String parsed = command
                    .replace("%player%", player.getName())
                    .replace("%item_id%", itemId)
                    .replace("%slot%", slotName);

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