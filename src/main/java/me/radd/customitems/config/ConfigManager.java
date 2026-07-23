package me.radd.customitems.config;

import me.radd.customitems.RaddItemsPlugin;
import me.radd.customitems.items.CustomItemAttributeDefinition;
import me.radd.customitems.items.CustomItemDefinition;
import me.radd.customitems.items.CustomItemEnchantmentDefinition;
import me.radd.customitems.items.CustomItemRegistry;
import me.radd.customitems.items.CustomPotionEffectDefinition;
import me.radd.customitems.items.EquipEffectsDefinition;
import me.radd.customitems.items.ItemTriggersDefinition;
import me.radd.customitems.items.TriggerEffectDefinition;
import me.radd.customitems.sets.ItemSetDefinition;
import me.radd.customitems.sets.ItemSetRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static final int DEFAULT_TRIGGER_DURATION = 100;
    private static final int CONTINUOUS_EFFECT_CONFIG_DURATION = -1;

    private static final TriggerEffectDefinition DISABLED_TRIGGER =
            new TriggerEffectDefinition(false, List.of(), List.of(), List.of());

    private static final EquipEffectsDefinition DISABLED_EQUIP_EFFECTS =
            new EquipEffectsDefinition(false, List.of(), List.of(), List.of());

    private final RaddItemsPlugin plugin;
    private final CustomItemRegistry itemRegistry;
    private final ItemSetRegistry setRegistry;

    public ConfigManager(RaddItemsPlugin plugin, CustomItemRegistry itemRegistry, ItemSetRegistry setRegistry) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.setRegistry = setRegistry;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.debugLog("Loading configuration files...");

        itemRegistry.clear();
        setRegistry.clear();

        FileConfiguration itemsConfig = loadItemsConfig();
        loadItems(itemsConfig);
        loadSets(itemsConfig);

        plugin.debugLog("Configuration load completed.");
    }

    private FileConfiguration loadItemsConfig() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder: " + dataFolder.getAbsolutePath());
        }

        File itemsFile = new File(dataFolder, "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
            plugin.debugLog("Created default items.yml");
        }

        return YamlConfiguration.loadConfiguration(itemsFile);
    }

    private void loadItems(FileConfiguration itemsConfig) {
        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.debugLog("No 'items' section found in items.yml");
            return;
        }

        int loaded = 0;

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection sec = itemsSection.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }

            try {
                CustomItemDefinition def = parseItemDefinition(id, sec);
                itemRegistry.register(def);
                loaded++;
                plugin.debugLog("Loaded item: " + id);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping invalid item '" + id + "': " + ex.getMessage());
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipping item '" + id + "' due to unexpected error: " + ex.getMessage());
            }
        }

        plugin.debugLog("Total items loaded: " + loaded);
    }

    private CustomItemDefinition parseItemDefinition(String id, ConfigurationSection sec) {
        String matName = sec.getString("material", "STONE");
        Material material = Material.matchMaterial(matName);
        if (material == null) {
            plugin.debugLog("Invalid material '" + matName + "' for item '" + id + "', using STONE.");
            material = Material.STONE;
        }

        String name = sec.getString("name");
        List<String> lore = sec.getStringList("lore");
        int customModelData = sec.getInt("custom_model_data", 0);
        boolean unbreakable = sec.getBoolean("unbreakable", false);
        List<String> itemFlags = sec.getStringList("item_flags");

        List<CustomItemEnchantmentDefinition> enchantments = parseEnchantments(sec.getList("enchantments"), id);
        List<CustomItemAttributeDefinition> attributes = parseAttributes(sec.getList("attributes"), id, false);

        EquipEffectsDefinition equipEffects = loadEquipEffects(sec.getConfigurationSection("equip_effects"));
        ItemTriggersDefinition triggers = loadTriggers(sec.getConfigurationSection("triggers"));

        return new CustomItemDefinition(
                id,
                material,
                name,
                lore,
                customModelData,
                unbreakable,
                itemFlags,
                enchantments,
                attributes,
                equipEffects,
                triggers
        );
    }

    private void loadSets(FileConfiguration itemsConfig) {
        ConfigurationSection setsSection = itemsConfig.getConfigurationSection("sets");
        if (setsSection == null) {
            plugin.debugLog("No 'sets' section found in items.yml");
            return;
        }

        int loaded = 0;

        for (String id : setsSection.getKeys(false)) {
            ConfigurationSection sec = setsSection.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }

            try {
                ItemSetDefinition def = parseSetDefinition(id, sec);
                setRegistry.register(def);
                loaded++;
                plugin.debugLog("Loaded set: " + id + " (" + def.getPieces().size() + " pieces)");
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping invalid set '" + id + "': " + ex.getMessage());
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipping set '" + id + "' due to unexpected error: " + ex.getMessage());
            }
        }

        plugin.debugLog("Total sets loaded: " + loaded);
    }

    private ItemSetDefinition parseSetDefinition(String id, ConfigurationSection sec) {
        List<String> pieces = sec.getStringList("pieces");
        int requiredAmount = sec.getInt("required_amount", 1);
        List<CustomPotionEffectDefinition> potionEffects = loadContinuousPotionEffects(sec.getList("potion_effects"));
        List<CustomItemAttributeDefinition> attributes = parseAttributes(sec.getList("attributes"), id, true);

        ConfigurationSection triggersSec = sec.getConfigurationSection("triggers");
        TriggerEffectDefinition onActivate = triggersSec != null
                ? loadTrigger(triggersSec.getConfigurationSection("on_activate"))
                : DISABLED_TRIGGER;
        TriggerEffectDefinition onDeactivate = triggersSec != null
                ? loadTrigger(triggersSec.getConfigurationSection("on_deactivate"))
                : DISABLED_TRIGGER;

        return new ItemSetDefinition(
                id,
                pieces,
                requiredAmount,
                potionEffects,
                attributes,
                onActivate,
                onDeactivate
        );
    }

    private List<CustomItemEnchantmentDefinition> parseEnchantments(List<?> rawEnchantments, String itemId) {
        List<CustomItemEnchantmentDefinition> enchantments = new ArrayList<>();
        if (rawEnchantments == null) {
            return enchantments;
        }

        for (Object obj : rawEnchantments) {
            if (!(obj instanceof Map<?, ?> map)) {
                continue;
            }

            try {
                Object typeObj = map.get("type");
                if (typeObj == null) {
                    plugin.debugLog("Skipping enchantment without type in item '" + itemId + "'");
                    continue;
                }

                Object levelObj = map.get("level");
                String type = String.valueOf(typeObj);
                int level = levelObj instanceof Number number ? number.intValue() : 1;

                enchantments.add(new CustomItemEnchantmentDefinition(type, level));
            } catch (IllegalArgumentException ex) {
                plugin.debugLog("Skipping invalid enchantment in item '" + itemId + "': " + ex.getMessage());
            }
        }

        return enchantments;
    }

    private List<CustomItemAttributeDefinition> parseAttributes(List<?> rawAttributes, String ownerId, boolean setDefaultsSlotAny) {
        List<CustomItemAttributeDefinition> attributes = new ArrayList<>();
        if (rawAttributes == null) {
            return attributes;
        }

        for (Object obj : rawAttributes) {
            if (!(obj instanceof Map<?, ?> map)) {
                continue;
            }

            try {
                Object attributeObj = map.get("attribute");
                if (attributeObj == null) {
                    attributeObj = map.get("type");
                }

                Object amountObj = map.get("amount");
                Object operationObj = map.get("operation");
                Object slotObj = map.get("slot");

                if (attributeObj == null || amountObj == null || operationObj == null || (!setDefaultsSlotAny && slotObj == null)) {
                    plugin.debugLog("Skipping invalid attribute entry in '" + ownerId + "'");
                    continue;
                }

                if (!(amountObj instanceof Number number)) {
                    plugin.debugLog("Invalid attribute amount in '" + ownerId + "'");
                    continue;
                }

                String attribute = String.valueOf(attributeObj);
                double amount = number.doubleValue();
                String operation = String.valueOf(operationObj);
                String slot = slotObj != null ? String.valueOf(slotObj) : "ANY";

                attributes.add(new CustomItemAttributeDefinition(attribute, amount, operation, slot));
            } catch (IllegalArgumentException ex) {
                plugin.debugLog("Skipping invalid attribute in '" + ownerId + "': " + ex.getMessage());
            }
        }

        return attributes;
    }

    private EquipEffectsDefinition loadEquipEffects(ConfigurationSection sec) {
        if (sec == null) {
            return DISABLED_EQUIP_EFFECTS;
        }

        boolean enabled = sec.getBoolean("enabled", false);
        List<String> activeSlots = sec.getStringList("active_slots");
        List<String> commands = sec.getStringList("commands");
        List<CustomPotionEffectDefinition> potionEffects = loadContinuousPotionEffects(sec.getList("potion_effects"));

        return new EquipEffectsDefinition(enabled, activeSlots, potionEffects, commands);
    }

    private ItemTriggersDefinition loadTriggers(ConfigurationSection sec) {
        if (sec == null) {
            return new ItemTriggersDefinition();
        }

        return new ItemTriggersDefinition(
                loadTrigger(sec.getConfigurationSection("on_equip")),
                loadTrigger(sec.getConfigurationSection("on_unequip")),
                loadTrigger(sec.getConfigurationSection("on_hit")),
                loadTrigger(sec.getConfigurationSection("on_kill")),
                loadTrigger(sec.getConfigurationSection("on_consume"))
        );
    }

    private TriggerEffectDefinition loadTrigger(ConfigurationSection sec) {
        if (sec == null) {
            return DISABLED_TRIGGER;
        }

        boolean enabled = sec.getBoolean("enabled", false);
        List<CustomPotionEffectDefinition> potionEffectsSelf = loadTriggerPotionEffects(sec.getList("potion_effects_self"));
        List<CustomPotionEffectDefinition> potionEffectsTarget = loadTriggerPotionEffects(sec.getList("potion_effects_target"));
        List<String> commands = sec.getStringList("commands");

        return new TriggerEffectDefinition(enabled, potionEffectsSelf, potionEffectsTarget, commands);
    }

    private List<CustomPotionEffectDefinition> loadContinuousPotionEffects(List<?> rawList) {
        return loadPotionEffects(rawList, false);
    }

    private List<CustomPotionEffectDefinition> loadTriggerPotionEffects(List<?> rawList) {
        return loadPotionEffects(rawList, true);
    }

    private List<CustomPotionEffectDefinition> loadPotionEffects(List<?> rawList, boolean durationRequired) {
        List<CustomPotionEffectDefinition> effects = new ArrayList<>();
        if (rawList == null) {
            return effects;
        }

        for (Object obj : rawList) {
            if (!(obj instanceof Map<?, ?> map)) {
                continue;
            }

            try {
                Object typeObj = map.get("type");
                if (typeObj == null) {
                    continue;
                }

                Object durationObj = map.get("duration");
                Object amplifierObj = map.get("amplifier");
                Object ambientObj = map.get("ambient");
                Object particlesObj = map.get("particles");
                Object iconObj = map.get("icon");

                String type = String.valueOf(typeObj);

                int duration = durationRequired ? DEFAULT_TRIGGER_DURATION : CONTINUOUS_EFFECT_CONFIG_DURATION;
                if (durationObj instanceof Number number) {
                    duration = number.intValue();
                }

                int amplifier = 0;
                if (amplifierObj instanceof Number number) {
                    amplifier = number.intValue();
                }

                boolean ambient = ambientObj instanceof Boolean b && b;
                boolean particles = !(particlesObj instanceof Boolean b) || b;
                boolean icon = !(iconObj instanceof Boolean b) || b;

                effects.add(new CustomPotionEffectDefinition(
                        type,
                        duration,
                        amplifier,
                        ambient,
                        particles,
                        icon
                ));
            } catch (IllegalArgumentException ex) {
                plugin.debugLog("Skipping invalid potion effect entry: " + ex.getMessage());
            }
        }

        return effects;
    }
}