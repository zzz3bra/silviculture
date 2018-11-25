package com.zzz3bra.silviculture.data.gathering;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Search {
    private Integer minYear;
    private Integer maxYear;
    private String manufacturer;
    private String modelName;
}
