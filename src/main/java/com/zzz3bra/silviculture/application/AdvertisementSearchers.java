package com.zzz3bra.silviculture.application;

import com.zzz3bra.silviculture.adapter.in.onliner.OnlinerSearcher;
import com.zzz3bra.silviculture.domain.Ad;
import com.zzz3bra.silviculture.domain.Search;

import java.util.List;
import java.util.Map;

public enum AdvertisementSearchers implements Searcher {
    ONLINER(new OnlinerSearcher());

    private final Searcher searcher;

    AdvertisementSearchers(Searcher searcher) {
        this.searcher = searcher;
    }

    @Override
    public String getTechnicalName() {
        return searcher.getTechnicalName();
    }

    @Override
    public List<Ad> find(Search search) {
        return searcher.find(search);
    }

    @Override
    public Map<String, List<String>> supportedManufacturersAndModels() {
        return searcher.supportedManufacturersAndModels();
    }
}
