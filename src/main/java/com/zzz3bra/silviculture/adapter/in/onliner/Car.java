package com.zzz3bra.silviculture.adapter.in.onliner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Car {
    private long odometerState;
    private long year;
    private CarModel model;
    @JsonIgnore
    private int costInUsd;
}
