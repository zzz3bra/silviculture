package com.zzz3bra.silviculture.data.gathering;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
public class Search {
    private final Integer minYear;
    private final Integer maxYear;
    private final String manufacturer;
    private final String modelName;
}
