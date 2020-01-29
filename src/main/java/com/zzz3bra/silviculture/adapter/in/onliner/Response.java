package com.zzz3bra.silviculture.adapter.in.onliner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
    private List<Advertisement> adverts;
    private String message;
    private Map<String, List<String>> errors;
}
