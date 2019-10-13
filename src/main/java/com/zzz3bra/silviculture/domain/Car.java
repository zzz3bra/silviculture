package com.zzz3bra.silviculture.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Car {
    public final String manufacturer;
    public final String model;
    public final long manufacturedYear;
    public final long mileageInKilometers;
    public final long costInUsd;

    public Car(String manufacturer, String model, long manufacturedYear, long mileageInKilometers, long costInUsd) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.manufacturedYear = manufacturedYear;
        this.mileageInKilometers = mileageInKilometers;
        this.costInUsd = costInUsd;
    }
}
