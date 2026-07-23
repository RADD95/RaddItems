package me.radd.customitems.sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ItemSetRegistry {

    private final Map<String, ItemSetDefinition> sets = new HashMap<>();
    private final Map<String, Set<String>> pieceToSetIds = new HashMap<>();

    public void register(ItemSetDefinition def) {
        Objects.requireNonNull(def, "def");
        Objects.requireNonNull(def.getId(), "def.id");

        ItemSetDefinition previous = sets.put(def.getId(), def);
        if (previous != null) {
            unregisterIndex(previous);
        }

        index(def);
    }

    public ItemSetDefinition get(String id) {
        return sets.get(id);
    }

    public boolean exists(String id) {
        return sets.containsKey(id);
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(sets.keySet());
    }

    public Set<String> getCandidateSetIdsForPiece(String pieceId) {
        if (pieceId == null) {
            return Collections.emptySet();
        }

        Set<String> setIds = pieceToSetIds.get(pieceId);
        if (setIds == null || setIds.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(setIds);
    }

    public void clear() {
        sets.clear();
        pieceToSetIds.clear();
    }

    private void index(ItemSetDefinition def) {
        if (def.getPieces() == null || def.getPieces().isEmpty()) {
            return;
        }

        for (String pieceId : def.getPieces()) {
            if (pieceId == null || pieceId.isBlank()) {
                continue;
            }

            pieceToSetIds
                    .computeIfAbsent(pieceId, ignored -> new HashSet<>())
                    .add(def.getId());
        }
    }

    private void unregisterIndex(ItemSetDefinition def) {
        if (def.getPieces() == null || def.getPieces().isEmpty()) {
            return;
        }

        for (String pieceId : def.getPieces()) {
            if (pieceId == null || pieceId.isBlank()) {
                continue;
            }

            Set<String> setIds = pieceToSetIds.get(pieceId);
            if (setIds == null) {
                continue;
            }

            setIds.remove(def.getId());
            if (setIds.isEmpty()) {
                pieceToSetIds.remove(pieceId);
            }
        }
    }
}