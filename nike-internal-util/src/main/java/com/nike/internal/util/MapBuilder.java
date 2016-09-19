package com.nike.internal.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple fluent map builder that allows you to easily initialize static map fields with hardcoded data. We could use
 * the Guava library's ImmutableMap, but there's no reason to require pulling in all of Guava just for this one simple
 * thing.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public class MapBuilder<K, V> {

    private final Map<K, V> map = new HashMap<>();

    private MapBuilder() { /* private to enforce builder pattern */ }

    public static <K, V> MapBuilder<K, V> builder() {
        return new MapBuilder<>();
    }

    public static <K, V> MapBuilder<K, V> builder(K firstKey, V firstVal) {
        MapBuilder<K, V> builder = new MapBuilder<>();
        builder.put(firstKey, firstVal);
        return builder;
    }

    public MapBuilder<K, V> put(K key, V value) {
        map.put(key, value);
        return this;
    }

    public MapBuilder<K, V> putAll(Map<K, V> otherMap) {
        for (Map.Entry<K, V> entry : otherMap.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Map<K, V> build() {
        return new HashMap<>(map);
    }
}
