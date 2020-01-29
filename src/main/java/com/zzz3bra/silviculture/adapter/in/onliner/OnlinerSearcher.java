package com.zzz3bra.silviculture.adapter.in.onliner;

import com.zzz3bra.silviculture.application.Searcher;
import com.zzz3bra.silviculture.domain.Ad;
import com.zzz3bra.silviculture.domain.Car;
import com.zzz3bra.silviculture.domain.Search;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.restassured.RestAssured.with;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.tuple.Pair.of;

public class OnlinerSearcher implements Searcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnlinerSearcher.class);
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
    // Map<Pair<ManufacturerName,ManufacturerId>, List<Pair<ModelName,ModelId>>>

    private static Map<Pair<String, String>, List<Pair<String, String>>> parseIds() {
        Map<Pair<String, String>, List<Pair<String, String>>> result = new TreeMap<>();
        final Manufacturer[] manufacturers = with().get("https://ab.api.onliner.by/dictionaries/manufacturer").getBody().as(Manufacturer[].class, ObjectMapperType.JACKSON_2);
        for (Manufacturer manufacturer : manufacturers) {
            final Model[] models = with().queryParam("manufacturer", manufacturer.getId()).get("https://ab.api.onliner.by/dictionaries/model").getBody().as(Model[].class, ObjectMapperType.JACKSON_2);
            result.put(of(manufacturer.getName().toLowerCase(), valueOf(manufacturer.getId())), stream(models).map(m -> of(m.getName().toLowerCase(), valueOf(m.getId()))).collect(toList()));
        }
        return result;
    }

    private static String readFile(String filename) throws IOException {
        File file = new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public OnlinerSearcher() {
        this.ids = parseIds();
    }

    @Override
    public List<Ad> find(Search search) {
        Map<String, String> parametersMap = new HashMap<>(defaultParameters);

        Optional<Pair<String, String>> manufacturerId = ofNullable(search.getManufacturer()).map(manufacturer -> ids.keySet().stream().filter(pair -> pair.getKey().equalsIgnoreCase(manufacturer)).findFirst().orElse(null));
        manufacturerId.ifPresent(manufacturer -> {
            final String modelId = ofNullable(search.getModelName()).map(model -> ids.get(manufacturerId.get()).stream().filter(pair -> pair.getKey().equalsIgnoreCase(model)).findFirst().map(Pair::getValue).orElse(null)).orElse("");
            parametersMap.put("car[0][manufacturer]", manufacturer.getValue());
            parametersMap.put("car[0][model]", modelId);
        });
        ofNullable(search.getMaxYear()).ifPresent(maxYear -> parametersMap.put("max-year", maxYear.toString()));
        ofNullable(search.getMinYear()).ifPresent(minYear -> parametersMap.put("min-year", minYear.toString()));

        Response result = with().queryParams(parametersMap).contentType(ContentType.URLENC).get("https://ab.onliner.by/sdapi/ab.api/search/vehicles").getBody().as(Response.class, ObjectMapperType.JACKSON_2);
        if (StringUtils.isNotBlank(result.getMessage()) && StringUtils.isNotBlank(result.getErrors())) {
            LOGGER.error("{} {}", result.getMessage(), result.getErrors());
            LOGGER.error("request parameters: {}", parametersMap);
            LOGGER.error("Search: {}", search);
            return Collections.emptyList();
        }
        return result.getAdverts().stream().map(ad -> {
            final long costInUsd = Double.valueOf(ad.getPrice().getAmount()).longValue();//hopefully most of cars will use USD
            final long mileageInKilometers = ad.getSpecs().getOdometer().getValue();//hopefully most of cars will use kilometers
            Car car = new Car(ad.getManufacturer().getName(), ad.getModel().getName(), ad.getSpecs().getYear(), mileageInKilometers, costInUsd);
            return new Ad(String.valueOf(ad.getId()), ad.getTitle(), car, Stream.of(ad.getPhotos()).map(photo -> URI.create(photo.getImages().getOriginal())).collect(toList()), URI.create(ad.getHtml_url()));
        }).collect(toList());
    }

    @Override
    public Map<String, List<String>> supportedManufacturersAndModels() {
        return ids.entrySet().stream().collect(toMap(entry -> entry.getKey().getKey(), entry -> entry.getValue().stream().map(Pair::getKey).collect(toList())));
    }

    @Override
    public String getTechnicalName() {
        return "ONLINER";
    }
}
