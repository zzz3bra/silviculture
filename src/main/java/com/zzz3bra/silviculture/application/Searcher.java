package com.zzz3bra.silviculture.application;

import com.zzz3bra.silviculture.domain.Ad;
import com.zzz3bra.silviculture.domain.Search;

import java.util.List;
import java.util.Map;

public interface Searcher {
    String getTechnicalName();

    List<Ad> find(Search search);

    Map<String, List<String>> supportedManufacturersAndModels();
}
