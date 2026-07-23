package me.radd.customitems.items;

import java.util.Objects;

public final class CustomItemEnchantmentDefinition {

    private final String type;
    private final int level;

    public CustomItemEnchantmentDefinition(String type, int level) {
        this.type = Objects.requireNonNull(type, "type");

        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }

        this.level = level;
    }

    public String getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }
}