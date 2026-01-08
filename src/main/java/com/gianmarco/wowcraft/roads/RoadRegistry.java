package com.gianmarco.wowcraft.roads;

import net.minecraft.core.BlockPos;

import java.util.*;

public class RoadRegistry {
    private final Map<UUID, RoadNode> nodes = new LinkedHashMap<>();
    private final Map<Long, UUID> nodeByPos = new HashMap<>();
    private final Set<String> builtPairs = new HashSet<>();

    public void clear() {
        nodes.clear();
        nodeByPos.clear();
        builtPairs.clear();
    }

    public boolean addNode(RoadNode node) {
        if (nodes.containsKey(node.getId())) {
            return false;
        }

        long key = node.getPosition().asLong();
        if (nodeByPos.containsKey(key)) {
            return false;
        }
        nodes.put(node.getId(), node);
        nodeByPos.put(key, node.getId());
        return true;
    }

    public RoadNode getNode(UUID id) {
        return nodes.get(id);
    }

    public Collection<RoadNode> getNodes() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public boolean hasBuiltPair(UUID a, UUID b) {
        return builtPairs.contains(pairKey(a, b));
    }

    public boolean addBuiltPair(UUID a, UUID b) {
        return builtPairs.add(pairKey(a, b));
    }

    public Set<String> getBuiltPairs() {
        return Collections.unmodifiableSet(builtPairs);
    }

    public void addBuiltPairKey(String key) {
        builtPairs.add(key);
    }

    private static String pairKey(UUID a, UUID b) {
        if (a.compareTo(b) < 0) {
            return a + "-" + b;
        }
        return b + "-" + a;
    }
}
