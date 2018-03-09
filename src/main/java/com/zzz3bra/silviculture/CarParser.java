package com.zzz3bra.silviculture;

import com.zzz3bra.silviculture.domain.Advertisement;
import com.zzz3bra.silviculture.domain.Response;
import com.zzz3bra.silviculture.domain.Result;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.with;

public class CarParser {
    private CarParser() {
    }

    private static final Pattern COST_PATTERN = Pattern.compile("(?!\\s)[0-9 ]{2,}(?=\\$<br>)");
    private static final Entry<String, String> NISSAN_SILVIA = new HashMap.SimpleImmutableEntry<>("car[0][45][m]", "922");

    private static Map<String, String> parameters;

    public static Result getSilvias() {
        Map<String, String> parametersMap = getParameters();
        parametersMap.put(NISSAN_SILVIA.getKey(), NISSAN_SILVIA.getValue());
        return getResult(parametersMap);
    }

    public static Result getResult(Map<String, ?> parametersMap) {
        Result result = with().formParams(parametersMap).contentType(ContentType.URLENC).post("https://ab.onliner.by/search").getBody().as(Response.class, ObjectMapperType.JACKSON_2).getResult();
        Matcher matcher = COST_PATTERN.matcher(result.getContent());
        Iterator<Entry<String, Advertisement>> carsSet = result.getAdvertisements().entrySet().iterator();
        while (matcher.find()) {
            carsSet.next().getValue().getCar().setCostInUsd(Integer.valueOf(matcher.group().replace(" ", "")));
        }
        return result;
    }

    public static Map<String, String> getParameters() {
        if (parameters == null) {
            parameters = new HashMap<>();
            parameters.put("currency", "USD");
            parameters.put("sort[]", "creation_date");
            parameters.put("page", "1");
        }
        return new HashMap<>(parameters);
    }

}
