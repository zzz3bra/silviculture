package com.zzz3bra.silviculture.data.gathering;

import com.zzz3bra.silviculture.data.Ad;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdvertisementFinder {

    public List<Ad> find(Search search, AdvertisementSearchers... advertisementSearchers) {
        return Stream.of(advertisementSearchers).map(advertisementSearcher -> advertisementSearcher.searcher.find(search)).flatMap(List::stream).collect(Collectors.toList());
    }

    public List<Ad> find(Search search) {
        return find(search, AdvertisementSearchers.values());
    }

    public enum AdvertisementSearchers implements Searcher {
        ONLINER(new OnlinerSearcher("onliner/Manufacturers.json", "onliner/ManufacturersModel.json"));

        private final Searcher searcher;

        AdvertisementSearchers(Searcher searcher) {
            this.searcher = searcher;
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
}
