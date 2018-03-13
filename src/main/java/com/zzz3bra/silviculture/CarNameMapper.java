package com.zzz3bra.silviculture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class CarNameMapper {

    static Map<Pair<String, String>, List<Pair<String, String>>> parseIds(String manufacturers, String manModels) throws IOException {
        // holy shit, never used streams in such crappy way
        Map<Pair<String, String>, List<Pair<String, String>>> result = new TreeMap<>();
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>[]> manRef = new TypeReference<Map<String, String>[]>() {
        };
        Map<String, String>[] manMap = mapper.readValue(manufacturers, manRef);
        TypeReference<Map<String, Map<String, JsonNode>[]>> manModRef = new TypeReference<Map<String, Map<String, JsonNode>[]>>() {
        };
        Map<String, Map<String, JsonNode>[]> manModMap = mapper.readValue(manModels, manModRef);
        Arrays.stream(manMap).map(Map::entrySet).map(Set::iterator).map(entryIterator -> {
            Entry<String, String> entry1 = entryIterator.next();
            Entry<String, String> entry2 = entryIterator.next();
            if (entry1.getKey().equals("id")) {
                return new MutablePair<>(entry2.getValue(), entry1.getValue());
            }
            return new MutablePair<>(entry1.getValue(), entry2.getValue());
        }).forEach(pair -> {
            Map<String, JsonNode>[] mapWithModels = manModMap.get(pair.getValue());
            Map<String, String> mapToBeParsed = new HashMap<>();
            for (Map<String, JsonNode> map : mapWithModels) {
                if (map.entrySet().iterator().next().getValue().isArray()) {
                    mapToBeParsed.put(map.entrySet().iterator().next().getKey(), map.entrySet().iterator().next().getValue().get(0).textValue());
                    map.entrySet().iterator().next().getValue().get(1).fields().forEachRemaining(entry -> mapToBeParsed.put(entry.getKey(), entry.getValue().textValue()));
                } else {
                    map.forEach((key, value) -> mapToBeParsed.put(key, value.textValue()));
                }
            }
            pair.setLeft(pair.getLeft().toLowerCase());
            pair.setRight(pair.getRight().toLowerCase());
            result.put(pair, mapToBeParsed.entrySet().stream().map(entry -> new MutablePair<>(entry.getKey().toLowerCase(), entry.getValue().toLowerCase())).collect(Collectors.toList()));
        });
        return result;
    }

}
