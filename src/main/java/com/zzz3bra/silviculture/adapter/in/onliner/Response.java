package com.zzz3bra.silviculture.adapter.in.onliner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response {
    private List<Advertisement> adverts;
    private String message;
    private String errors;
}
