package me.radd.customitems.listeners;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.CustomPotionEffectDefinition;
import me.radd.customitems.items.ItemTriggersDefinition;
import me.radd.customitems.items.TriggerEffectDefinition;
import me.radd.customitems.util.PdcKeys;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

public class ItemUseListener implements Listener {

    private final RaddItemsPlugin plugin;

    public ItemUseListener(RaddItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        String itemId = getCustomItemId(item);
        if (itemId == null) {
            return;
        }

        CustomItemDefinition def = plugin.getItemRegistry().get(itemId);
        if (def == null) {
            plugin.debugLog("on_consume skipped: item '" + itemId + "' not found in registry.");
            return;
        }

        ItemTriggersDefinition triggers = def.getTriggers();
        if (triggers == null) {
            return;
        }

        TriggerEffectDefinition onConsume = triggers.getOnConsume();
        if (onConsume == null || !onConsume.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        applySelfEffects(player, onConsume.getPotionEffectsSelf(), itemId);
        executeCommands(player, onConsume.getCommands(), itemId);
    }

    private void applySelfEffects(Player player, List<CustomPotionEffectDefinition> effects, String itemId) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (CustomPotionEffectDefinition effectDef : effects) {
            if (effectDef == null || effectDef.getType() == null) {
                continue;
            }

            PotionEffectType type = parsePotionEffectType(effectDef.getType());
            if (type == null) {
                plugin.debugLog("Invalid consume effect type '" + effectDef.getType()
                        + "' for item '" + itemId + "'.");
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

    private void executeCommands(Player player, List<String> commands, String itemId) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        CommandSender console = Bukkit.getServer().getConsoleSender();

        for (String command : commands) {
            String parsed = command
                    .replace("%player%", player.getName())
                    .replace("%item_id%", itemId);

            Bukkit.dispatchCommand(console, parsed);
        }
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

            if (key == null) return null;
            return Registry.EFFECT.get(key);
        } catch (Exception ignored) {
            return null;
        }
    }
}