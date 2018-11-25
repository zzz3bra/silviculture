package com.zzz3bra.silviculture.data.gathering;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.persistence.Entity;

@Entity
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class Search {
    private final Integer minYear;
    private final Integer maxYear;
    private final String manufacturer;
    private final String modelName;
}
