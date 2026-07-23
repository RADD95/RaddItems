package me.radd.customitems.items;

import java.util.List;

public final class TriggerEffectDefinition {

    private final boolean enabled;
    private final List<CustomPotionEffectDefinition> potionEffectsSelf;
    private final List<CustomPotionEffectDefinition> potionEffectsTarget;
    private final List<String> commands;

    public TriggerEffectDefinition(
            boolean enabled,
            List<CustomPotionEffectDefinition> potionEffectsSelf,
            List<CustomPotionEffectDefinition> potionEffectsTarget,
            List<String> commands
    ) {
        this.enabled = enabled;
        this.potionEffectsSelf = potionEffectsSelf == null ? List.of() : List.copyOf(potionEffectsSelf);
        this.potionEffectsTarget = potionEffectsTarget == null ? List.of() : List.copyOf(potionEffectsTarget);
        this.commands = commands == null ? List.of() : List.copyOf(commands);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<CustomPotionEffectDefinition> getPotionEffectsSelf() {
        return potionEffectsSelf;
    }

    public List<CustomPotionEffectDefinition> getPotionEffectsTarget() {
        return potionEffectsTarget;
    }

    public List<String> getCommands() {
        return commands;
    }
}