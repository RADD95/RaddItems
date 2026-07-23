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
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;
import java.util.Locale;

public class CombatTriggerListener implements Listener {

    private final RaddItemsPlugin plugin;
    private final NamespacedKey projectileItemIdKey;

    public CombatTriggerListener(RaddItemsPlugin plugin) {
        this.plugin = plugin;
        this.projectileItemIdKey = new NamespacedKey(plugin, "projectile_custom_item_id");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player player)) {
            return;
        }

        String itemId = getCustomItemId(player.getInventory().getItemInMainHand());
        if (itemId == null) {
            return;
        }

        projectile.getPersistentDataContainer().set(
                projectileItemIdKey,
                PersistentDataType.STRING,
                itemId
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        CombatSource source = resolveCombatSource(event.getDamager());
        if (source == null) {
            return;
        }

        CustomItemDefinition def = plugin.getItemRegistry().get(source.itemId());
        if (def == null) {
            plugin.debugLog("Combat on_hit skipped: item id '" + source.itemId() + "' is not registered.");
            return;
        }

        TriggerEffectDefinition trigger = getEnabledTrigger(def, TriggerType.ON_HIT);
        if (trigger == null) {
            return;
        }

        Entity targetEntity = event.getEntity();

        applyEffectsToSelf(source.player(), trigger.getPotionEffectsSelf(), "on_hit", source.itemId());
        applyEffectsToTarget(targetEntity, trigger.getPotionEffectsTarget(), "on_hit", source.itemId());
        executeCommands(trigger.getCommands(), source.player(), targetEntity, source.itemId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        CombatSource source = resolveKillSource(event);
        if (source == null) {
            return;
        }

        CustomItemDefinition def = plugin.getItemRegistry().get(source.itemId());
        if (def == null) {
            plugin.debugLog("Combat on_kill skipped: item id '" + source.itemId() + "' is not registered.");
            return;
        }

        TriggerEffectDefinition trigger = getEnabledTrigger(def, TriggerType.ON_KILL);
        if (trigger == null) {
            return;
        }

        LivingEntity dead = event.getEntity();

        applyEffectsToSelf(source.player(), trigger.getPotionEffectsSelf(), "on_kill", source.itemId());
        applyEffectsToTarget(dead, trigger.getPotionEffectsTarget(), "on_kill", source.itemId());
        executeCommands(trigger.getCommands(), source.player(), dead, source.itemId());
    }

    private CombatSource resolveKillSource(EntityDeathEvent event) {
        DamageSource damageSource = event.getDamageSource();
        if (damageSource != null) {
            Entity causing = damageSource.getCausingEntity();
            Entity direct = damageSource.getDirectEntity();

            if (causing instanceof Player player) {
                if (direct instanceof Projectile projectile) {
                    String projectileItemId = projectile.getPersistentDataContainer().get(
                            projectileItemIdKey,
                            PersistentDataType.STRING
                    );

                    if (projectileItemId != null) {
                        return new CombatSource(player, projectileItemId);
                    }
                }

                String heldItemId = getCustomItemId(player.getInventory().getItemInMainHand());
                if (heldItemId != null) {
                    return new CombatSource(player, heldItemId);
                }
            }
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return null;
        }

        String itemId = getCustomItemId(killer.getInventory().getItemInMainHand());
        if (itemId == null) {
            return null;
        }

        return new CombatSource(killer, itemId);
    }

    private CombatSource resolveCombatSource(Entity damager) {
        if (damager instanceof Player player) {
            String itemId = getCustomItemId(player.getInventory().getItemInMainHand());
            if (itemId == null) {
                return null;
            }
            return new CombatSource(player, itemId);
        }

        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (!(shooter instanceof Player player)) {
                return null;
            }

            String itemId = projectile.getPersistentDataContainer().get(
                    projectileItemIdKey,
                    PersistentDataType.STRING
            );

            if (itemId == null) {
                itemId = getCustomItemId(player.getInventory().getItemInMainHand());
            }

            if (itemId == null) {
                return null;
            }

            return new CombatSource(player, itemId);
        }

        return null;
    }

    private TriggerEffectDefinition getEnabledTrigger(CustomItemDefinition def, TriggerType type) {
        ItemTriggersDefinition triggers = def.getTriggers();
        if (triggers == null) {
            return null;
        }

        TriggerEffectDefinition trigger = switch (type) {
            case ON_HIT -> triggers.getOnHit();
            case ON_KILL -> triggers.getOnKill();
        };

        return trigger != null && trigger.isEnabled() ? trigger : null;
    }

    private void applyEffectsToSelf(Player player, List<CustomPotionEffectDefinition> effects, String triggerName, String itemId) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (CustomPotionEffectDefinition effectDef : effects) {
            if (effectDef == null || effectDef.getType() == null) {
                continue;
            }

            PotionEffectType type = parsePotionEffectType(effectDef.getType());
            if (type == null) {
                plugin.debugLog("Invalid self effect type '" + effectDef.getType() + "' in " + triggerName
                        + " for item '" + itemId + "'.");
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

    private void applyEffectsToTarget(Entity targetEntity, List<CustomPotionEffectDefinition> effects, String triggerName, String itemId) {
        if (!(targetEntity instanceof LivingEntity target)) {
            return;
        }

        if (effects == null || effects.isEmpty()) {
            return;
        }

        for (CustomPotionEffectDefinition effectDef : effects) {
            if (effectDef == null || effectDef.getType() == null) {
                continue;
            }

            PotionEffectType type = parsePotionEffectType(effectDef.getType());
            if (type == null) {
                plugin.debugLog("Invalid target effect type '" + effectDef.getType() + "' in " + triggerName
                        + " for item '" + itemId + "'.");
                continue;
            }

            target.addPotionEffect(new PotionEffect(
                    type,
                    effectDef.getDuration(),
                    effectDef.getAmplifier(),
                    effectDef.isAmbient(),
                    effectDef.hasParticles(),
                    effectDef.hasIcon()
            ));
        }
    }

    private void executeCommands(List<String> commands, Player player, Entity target, String itemId) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        CommandSender console = Bukkit.getServer().getConsoleSender();

        for (String command : commands) {
            String parsed = applyPlaceholders(command, player, target, itemId);
            Bukkit.dispatchCommand(console, parsed);
        }
    }

    private String applyPlaceholders(String command, Player player, Entity target, String itemId) {
        String targetName = target != null ? target.getName() : "unknown";

        return command
                .replace("%player%", player.getName())
                .replace("%target%", targetName)
                .replace("%item_id%", itemId);
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

    private record CombatSource(Player player, String itemId) {}

    private enum TriggerType {
        ON_HIT,
        ON_KILL
    }
}