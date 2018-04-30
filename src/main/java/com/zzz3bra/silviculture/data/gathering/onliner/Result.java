package com.zzz3bra.silviculture.data.gathering.onliner;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class Result {
    private Map<String, Object> counters;
    private Map<String, Advertisement> advertisements = new LinkedHashMap<>();
    private String content = "";
}
