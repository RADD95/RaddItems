package me.radd.customitems.sets;

import me.radd.customitems.items.CustomItemAttributeDefinition;
import me.radd.customitems.items.CustomPotionEffectDefinition;
import me.radd.customitems.items.TriggerEffectDefinition;

import java.util.List;
import java.util.Objects;

public final class ItemSetDefinition {

    private final String id;
    private final List<String> pieces;
    private final int requiredAmount;
    private final List<CustomPotionEffectDefinition> potionEffects;
    private final List<CustomItemAttributeDefinition> attributes;
    private final TriggerEffectDefinition onActivate;
    private final TriggerEffectDefinition onDeactivate;

    public ItemSetDefinition(
            String id,
            List<String> pieces,
            int requiredAmount,
            List<CustomPotionEffectDefinition> potionEffects,
            List<CustomItemAttributeDefinition> attributes,
            TriggerEffectDefinition onActivate,
            TriggerEffectDefinition onDeactivate
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.pieces = pieces == null ? List.of() : List.copyOf(pieces);
        this.potionEffects = potionEffects == null ? List.of() : List.copyOf(potionEffects);
        this.attributes = attributes == null ? List.of() : List.copyOf(attributes);
        this.onActivate = onActivate;
        this.onDeactivate = onDeactivate;

        if (requiredAmount < 1) {
            throw new IllegalArgumentException("requiredAmount must be at least 1 for set '" + id + "'.");
        }

        if (!this.pieces.isEmpty() && requiredAmount > this.pieces.size()) {
            throw new IllegalArgumentException(
                    "requiredAmount (" + requiredAmount + ") cannot be greater than pieces.size() ("
                            + this.pieces.size() + ") for set '" + id + "'."
            );
        }

        this.requiredAmount = requiredAmount;
    }

    public String getId() {
        return id;
    }

    public List<String> getPieces() {
        return pieces;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public List<CustomPotionEffectDefinition> getPotionEffects() {
        return potionEffects;
    }

    public List<CustomItemAttributeDefinition> getAttributes() {
        return attributes;
    }

    public TriggerEffectDefinition getOnActivate() {
        return onActivate;
    }

    public TriggerEffectDefinition getOnDeactivate() {
        return onDeactivate;
    }
}