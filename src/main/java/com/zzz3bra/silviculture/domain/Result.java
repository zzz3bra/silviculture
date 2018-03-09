package com.zzz3bra.silviculture.domain;

import lombok.Data;

import java.util.Map;

@Data
public class Result {
    private Map<String, Object> counters;
    private Map<String, Advertisement> advertisements;
    private String content;
}
