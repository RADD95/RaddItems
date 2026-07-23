package me.radd.customitems.items;

import java.util.Objects;

public final class CustomItemAttributeDefinition {

    private final String attribute;
    private final double amount;
    private final String operation;
    private final String slot;

    public CustomItemAttributeDefinition(String attribute, double amount, String operation, String slot) {
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.operation = Objects.requireNonNull(operation, "operation");
        this.slot = Objects.requireNonNull(slot, "slot");

        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("amount must be a finite number");
        }

        this.amount = amount;
    }

    public String getAttribute() {
        return attribute;
    }

    public double getAmount() {
        return amount;
    }

    public String getOperation() {
        return operation;
    }

    public String getSlot() {
        return slot;
    }
}