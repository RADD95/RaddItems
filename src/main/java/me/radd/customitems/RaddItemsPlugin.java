package me.radd.customitems;

import me.radd.customitems.commands.RaddItemsCommand;
import me.radd.customitems.config.ConfigManager;
import me.radd.customitems.items.CustomItemRegistry;
import me.radd.customitems.items.ItemFactory;
import me.radd.customitems.listeners.CombatTriggerListener;
import me.radd.customitems.listeners.EquipEffectsListener;
import me.radd.customitems.listeners.ItemEquipTriggerListener;
import me.radd.customitems.listeners.ItemUseListener;
import me.radd.customitems.listeners.SetEffectsListener;
import me.radd.customitems.listeners.SetTriggersListener;
import me.radd.customitems.sets.ItemSetRegistry;
import me.radd.customitems.util.PdcKeys;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class RaddItemsPlugin extends JavaPlugin {

    private CustomItemRegistry itemRegistry;
    private ItemSetRegistry setRegistry;
    private ItemFactory itemFactory;
    private ConfigManager configManager;

    private EquipEffectsListener equipEffectsListener;
    private ItemUseListener itemUseListener;
    private CombatTriggerListener combatTriggerListener;
    private ItemEquipTriggerListener itemEquipTriggerListener;
    private SetEffectsListener setEffectsListener;
    private SetTriggersListener setTriggersListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PdcKeys.init(this);

        setupCore();
        registerCommand();
        registerListeners();
        startTickListeners();

        logStartupBanner();
        debugLog("Plugin enabled.");
        debugLog("Items loaded: " + itemRegistry.getIds().size());
        debugLog("Sets loaded: " + setRegistry.getIds().size());
    }

    @Override
    public void onDisable() {
        stopTickListeners();
        HandlerList.unregisterAll(this);
        getLogger().info("RaddItems disabled.");
        debugLog("Plugin disabled.");
    }

    public void reloadPluginData() {
        stopTickListeners();
        reloadConfig();
        configManager.loadAll();
        startTickListeners();

        getLogger().info("Config, items and sets reloaded.");
        debugLog("Plugin data reloaded.");
        debugLog("Items loaded after reload: " + itemRegistry.getIds().size());
        debugLog("Sets loaded after reload: " + setRegistry.getIds().size());
    }

    private void setupCore() {
        this.itemRegistry = new CustomItemRegistry();
        this.setRegistry = new ItemSetRegistry();
        this.itemFactory = new ItemFactory(this, itemRegistry);
        this.configManager = new ConfigManager(this, itemRegistry, setRegistry);
        this.configManager.loadAll();
    }

    private void registerCommand() {
        RaddItemsCommand cmdExecutor = new RaddItemsCommand(this);

        if (getCommand("radditems") != null) {
            getCommand("radditems").setExecutor(cmdExecutor);
            getCommand("radditems").setTabCompleter(cmdExecutor);
            debugLog("Command '/radditems' registered successfully.");
        } else {
            getLogger().warning("Command 'radditems' is not defined in plugin.yml");
        }
    }

    private void registerListeners() {
        this.itemUseListener = new ItemUseListener(this);
        this.equipEffectsListener = new EquipEffectsListener(this);
        this.combatTriggerListener = new CombatTriggerListener(this);
        this.itemEquipTriggerListener = new ItemEquipTriggerListener(this);
        this.setEffectsListener = new SetEffectsListener(this);
        this.setTriggersListener = new SetTriggersListener(this);

        getServer().getPluginManager().registerEvents(itemUseListener, this);
        getServer().getPluginManager().registerEvents(equipEffectsListener, this);
        getServer().getPluginManager().registerEvents(combatTriggerListener, this);
        getServer().getPluginManager().registerEvents(itemEquipTriggerListener, this);
        getServer().getPluginManager().registerEvents(setEffectsListener, this);
        getServer().getPluginManager().registerEvents(setTriggersListener, this);

        debugLog("All listeners registered successfully.");
    }

    private void startTickListeners() {
        if (equipEffectsListener != null) {
            equipEffectsListener.start();
        }

        if (itemEquipTriggerListener != null) {
            itemEquipTriggerListener.start();
        }

        if (setEffectsListener != null) {
            setEffectsListener.start();
        }

        if (setTriggersListener != null) {
            setTriggersListener.start();
        }

        debugLog("Tick listeners started.");
    }

    private void stopTickListeners() {
        if (equipEffectsListener != null) {
            equipEffectsListener.stop();
        }

        if (itemEquipTriggerListener != null) {
            itemEquipTriggerListener.stop();
        }

        if (setEffectsListener != null) {
            setEffectsListener.stop();
        }

        if (setTriggersListener != null) {
            setTriggersListener.stop();
        }

        debugLog("Tick listeners stopped.");
    }

    private void logStartupBanner() {
        getLogger().info("██████╗  █████╗ ██████╗ ██████╗ ██╗████████╗███████╗███╗   ███╗███████╗");
        getLogger().info("██╔══██╗██╔══██╗██╔══██╗██╔══██╗██║╚══██╔══╝██╔════╝████╗ ████║██╔════╝");
        getLogger().info("██████╔╝███████║██║  ██║██║  ██║██║   ██║   █████╗  ██╔████╔██║███████╗");
        getLogger().info("██╔══██╗██╔══██║██║  ██║██║  ██║██║   ██║   ██╔══╝  ██║╚██╔╝██║╚════██║");
        getLogger().info("██║  ██║██║  ██║██████╔╝██████╔╝██║   ██║   ███████╗██║ ╚═╝ ██║███████║");
        getLogger().info("╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚═════╝ ╚═╝   ╚═╝   ╚══════╝╚═╝     ╚═╝╚══════╝");
        getLogger().info("");
        getLogger().info("RaddItems v" + getDescription().getVersion());
        getLogger().info("Running on " + getServer().getName());
        getLogger().info("Loaded " + itemRegistry.getIds().size() + " items and " + setRegistry.getIds().size() + " sets.");
        getLogger().info("");
    }

    public boolean isDebugEnabled() {
        return getConfig().getBoolean("debug", false);
    }

    public void debugLog(String message) {
        if (isDebugEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public CustomItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public ItemSetRegistry getSetRegistry() {
        return setRegistry;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}