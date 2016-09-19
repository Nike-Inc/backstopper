package com.nike.internal.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the functionality of {@link MapBuilder}.
 *
 * @author Nic Munroe
 */
public class MapBuilderTest {

    @Test
    public void builder_works_as_expected() {
        // given
        List<Pair<Integer, String>> valuesToPut = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            valuesToPut.add(Pair.of(i, UUID.randomUUID().toString()));
        }
        Map<Integer, String> mapToAdd = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            mapToAdd.put(i + 500, UUID.randomUUID().toString());
        }

        // when
        MapBuilder<Integer, String> builder = MapBuilder.builder();
        for (Pair<Integer, String> data : valuesToPut) {
            builder.put(data.getKey(), data.getValue());
        }
        builder.putAll(mapToAdd);
        Map<Integer, String> finalMap = builder.build();

        // then
        assertThat(finalMap).hasSize(valuesToPut.size() + mapToAdd.size());
        for (Pair<Integer, String> data : valuesToPut) {
            assertThat(finalMap.get(data.getKey())).isEqualTo(data.getValue());
        }
        for (Map.Entry<Integer, String> entry : mapToAdd.entrySet()) {
            assertThat(finalMap.get(entry.getKey())).isEqualTo(entry.getValue());
        }
    }

    @Test
    public void blank_builder_returns_empty_map() {
        // when
        Map<Integer, String> result = MapBuilder.<Integer, String>builder().build();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void firstKey_and_firstVal_builder_creator_method_works_as_expected() {
        // given
        Integer firstKey = 42;
        String firstValue = UUID.randomUUID().toString();
        Integer secondKey = 4242;
        String secondValue = UUID.randomUUID().toString();
        assertThat(firstKey).isNotEqualTo(secondKey);

        // when
        Map<Integer, String> result = MapBuilder
                .builder(firstKey, firstValue)
                .put(secondKey, secondValue)
                .build();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(firstKey)).isEqualTo(firstValue);
        assertThat(result.get(secondKey)).isEqualTo(secondValue);
    }

}