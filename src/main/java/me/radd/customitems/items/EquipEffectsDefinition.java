package me.radd.customitems.items;

import java.util.List;

public final class EquipEffectsDefinition {

    private final boolean enabled;
    private final List<String> activeSlots;
    private final List<CustomPotionEffectDefinition> potionEffects;
    private final List<String> commands;

    public EquipEffectsDefinition(
            boolean enabled,
            List<String> activeSlots,
            List<CustomPotionEffectDefinition> potionEffects,
            List<String> commands
    ) {
        this.enabled = enabled;
        this.activeSlots = activeSlots == null ? List.of() : List.copyOf(activeSlots);
        this.potionEffects = potionEffects == null ? List.of() : List.copyOf(potionEffects);
        this.commands = commands == null ? List.of() : List.copyOf(commands);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getActiveSlots() {
        return activeSlots;
    }

    public List<CustomPotionEffectDefinition> getPotionEffects() {
        return potionEffects;
    }

    public List<String> getCommands() {
        return commands;
    }
}