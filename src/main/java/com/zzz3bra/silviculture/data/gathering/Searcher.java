package com.zzz3bra.silviculture.data.gathering;

import com.zzz3bra.silviculture.data.Ad;

import java.util.List;
import java.util.Map;

public interface Searcher {
    List<Ad> find(Search search);

    Map<String, List<String>> supportedManufacturersAndModels();
}
