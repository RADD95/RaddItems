package me.radd.customitems.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class PdcKeys {

    // Evita instanciar
    private PdcKeys() {}

    public static NamespacedKey CUSTOM_ITEM_ID;

    public static void init(JavaPlugin plugin) {
        CUSTOM_ITEM_ID = new NamespacedKey(plugin, "custom_item_id");
    }
}