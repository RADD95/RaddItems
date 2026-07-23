package me.radd.customitems.items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CustomItemRegistry {

    private final Map<String, CustomItemDefinition> items = new HashMap<>();

    public void register(CustomItemDefinition def) {
        items.put(def.getId(), def);
    }

    public CustomItemDefinition get(String id) {
        return items.get(id);
    }

    public boolean exists(String id) {
        return items.containsKey(id);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(items.keySet());
    }

    public void clear() {
        items.clear();
    }
}