package com.zzz3bra.silviculture.data.gathering.onliner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Photo {
    private String comment;
    private Images images;
}