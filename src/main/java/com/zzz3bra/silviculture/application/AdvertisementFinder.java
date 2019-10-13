package com.zzz3bra.silviculture.application;

import com.zzz3bra.silviculture.domain.Ad;
import com.zzz3bra.silviculture.domain.Search;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdvertisementFinder {

    public List<Ad> find(Search search, AdvertisementSearchers... advertisementSearchers) {
        return Stream.of(advertisementSearchers).map(advertisementSearcher -> advertisementSearcher.find(search)).flatMap(List::stream).collect(Collectors.toList());
    }

    public List<Ad> find(Search search) {
        return find(search, AdvertisementSearchers.values());
    }

}
