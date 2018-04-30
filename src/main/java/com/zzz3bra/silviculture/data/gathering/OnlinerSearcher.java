package com.zzz3bra.silviculture.data.gathering;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzz3bra.silviculture.data.Ad;
import com.zzz3bra.silviculture.data.Car;
import com.zzz3bra.silviculture.data.gathering.onliner.Advertisement;
import com.zzz3bra.silviculture.data.gathering.onliner.Response;
import com.zzz3bra.silviculture.data.gathering.onliner.Result;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.with;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

class OnlinerSearcher implements Searcher {

    private static final Pattern COST_PATTERN = Pattern.compile("(?!\\s)[0-9 ]{2,}(?=\\$<br>)");
    private static final Map<String, String> defaultParameters;

    static {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("currency", "USD");
        parameters.put("sort[]", "creation_date");
        parameters.put("page", "1");
        defaultParameters = unmodifiableMap(parameters);
    }

    private final Map<Pair<String, String>, List<Pair<String, String>>> ids;

    private static Map<Pair<String, String>, List<Pair<String, String>>> parseIds(String manufacturers, String manModels) throws IOException {
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
            Map.Entry<String, String> entry1 = entryIterator.next();
            Map.Entry<String, String> entry2 = entryIterator.next();
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

    private static String readFile(String filename) throws IOException {
        File file = new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    OnlinerSearcher(String manufacturersJsonPath, String manModelsJsonPath) {
        try {
            this.ids = parseIds(readFile(manufacturersJsonPath), readFile(manModelsJsonPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Ad> find(Search search) {
        Map<String, String> parametersMap = new HashMap<>(defaultParameters);

        Optional<Pair<String, String>> manufacturerId = ofNullable(search.getManufacturer()).map(manufacturer -> ids.keySet().stream().filter(pair -> pair.getKey().equalsIgnoreCase(manufacturer)).findFirst().orElse(null));
        manufacturerId.ifPresent(manufacturer -> parametersMap.put("car[0][" + manufacturer.getValue() + "]", ofNullable(search.getModelName()).map(model -> ids.get(manufacturerId.get()).stream().filter(pair -> pair.getKey().equalsIgnoreCase(model)).findFirst().map(Pair::getValue).orElse(null)).orElse("")));
        ofNullable(search.getMaxYear()).ifPresent(maxYear -> parametersMap.put("max-year", maxYear.toString()));
        ofNullable(search.getMinYear()).ifPresent(minYear -> parametersMap.put("min-year", minYear.toString()));

        Result result = with().formParams(parametersMap).contentType(ContentType.URLENC).post("https://ab.onliner.by/search").getBody().as(Response.class, ObjectMapperType.JACKSON_2).getResult();
        Matcher matcher = COST_PATTERN.matcher(result.getContent());
        Iterator<Map.Entry<String, Advertisement>> carsSet = result.getAdvertisements().entrySet().iterator();
        while (matcher.find()) {
            carsSet.next().getValue().getCar().setCostInUsd(Integer.parseInt(matcher.group().replace(" ", "")));
        }
        return result.getAdvertisements().entrySet().stream().map(entry -> {
            Advertisement ad = entry.getValue();
            com.zzz3bra.silviculture.data.gathering.onliner.Car adCar = ad.getCar();
            Car car = new Car(adCar.getModel().getManufacturerName(), adCar.getModel().getName(), adCar.getYear(), adCar.getOdometerState(), adCar.getCostInUsd());
            return new Ad(entry.getKey(), ad.getTitle(), car, Stream.of(ad.getPhotos()).map(photo -> URI.create(photo.getImages().getOriginal())).collect(toList()));
        }).collect(toList());
    }

    @Override
    public Map<String, List<String>> supportedManufacturersAndModels() {
        return ids.entrySet().stream().collect(toMap(entry -> entry.getKey().getKey(), entry -> entry.getValue().stream().map(Pair::getKey).collect(toList())));
    }

}
