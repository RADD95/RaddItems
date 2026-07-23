package me.radd.customitems.items;

import java.util.Objects;

public final class CustomPotionEffectDefinition {

    public static final int UNSPECIFIED_DURATION = -1;

    private final String type;
    private final int duration;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;
    private final boolean icon;

    public CustomPotionEffectDefinition(
            String type,
            int duration,
            int amplifier,
            boolean ambient,
            boolean particles,
            boolean icon
    ) {
        this.type = Objects.requireNonNull(type, "type");

        if (duration < UNSPECIFIED_DURATION) {
            throw new IllegalArgumentException(
                    "duration must be " + UNSPECIFIED_DURATION + " (UNSPECIFIED_DURATION) or greater"
            );
        }

        if (amplifier < 0) {
            throw new IllegalArgumentException("amplifier must be 0 or greater");
        }

        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
        this.icon = icon;
    }

    public String getType() {
        return type;
    }

    public int getDuration() {
        return duration;
    }

    public boolean hasConfiguredDuration() {
        return duration >= 0;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public boolean isAmbient() {
        return ambient;
    }

    public boolean hasParticles() {
        return particles;
    }

    public boolean hasIcon() {
        return icon;
    }
}