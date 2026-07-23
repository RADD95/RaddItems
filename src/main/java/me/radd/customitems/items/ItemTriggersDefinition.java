package me.radd.customitems.items;

import java.util.List;
import java.util.Objects;

public final class ItemTriggersDefinition {

    private static final TriggerEffectDefinition DISABLED_TRIGGER =
            new TriggerEffectDefinition(false, List.of(), List.of(), List.of());

    private final TriggerEffectDefinition onEquip;
    private final TriggerEffectDefinition onUnequip;
    private final TriggerEffectDefinition onHit;
    private final TriggerEffectDefinition onKill;
    private final TriggerEffectDefinition onConsume;

    public ItemTriggersDefinition() {
        this(null, null, null, null, null);
    }

    public ItemTriggersDefinition(
            TriggerEffectDefinition onEquip,
            TriggerEffectDefinition onUnequip,
            TriggerEffectDefinition onHit,
            TriggerEffectDefinition onKill,
            TriggerEffectDefinition onConsume
    ) {
        this.onEquip = Objects.requireNonNullElse(onEquip, DISABLED_TRIGGER);
        this.onUnequip = Objects.requireNonNullElse(onUnequip, DISABLED_TRIGGER);
        this.onHit = Objects.requireNonNullElse(onHit, DISABLED_TRIGGER);
        this.onKill = Objects.requireNonNullElse(onKill, DISABLED_TRIGGER);
        this.onConsume = Objects.requireNonNullElse(onConsume, DISABLED_TRIGGER);
    }

    public TriggerEffectDefinition getOnEquip() {
        return onEquip;
    }

    public TriggerEffectDefinition getOnUnequip() {
        return onUnequip;
    }

    public TriggerEffectDefinition getOnHit() {
        return onHit;
    }

    public TriggerEffectDefinition getOnKill() {
        return onKill;
    }

    public TriggerEffectDefinition getOnConsume() {
        return onConsume;
    }
}