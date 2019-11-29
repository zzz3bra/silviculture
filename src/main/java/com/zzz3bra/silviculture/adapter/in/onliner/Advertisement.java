package com.zzz3bra.silviculture.adapter.in.onliner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Advertisement {
    private long id;
    private Price price;
    private String title;
    private String html_url;
    private Photo[] photos;
    private Specs specs;
    private Manufacturer manufacturer;
    private Model model;
}
