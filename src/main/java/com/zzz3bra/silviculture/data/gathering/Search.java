package com.zzz3bra.silviculture.data.gathering;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class Search implements Printable {
    private Integer minYear;
    private Integer maxYear;
    private String manufacturer;
    private String modelName;

    @Override
    @JsonIgnore
    public String getAsText() {
        return manufacturer + " " + modelName + " [" + (minYear == null ? "∞" : minYear) + ";" + (maxYear == null ? "∞" : maxYear) + "]";
    }
}
