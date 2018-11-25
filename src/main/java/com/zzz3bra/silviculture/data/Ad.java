package com.zzz3bra.silviculture.data;

import lombok.ToString;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;

@ToString
public class Ad {
    public final String id;
    public final String title;
    public final Car car;
    public final List<URI> carPhotos;
    public final URI link;

    public Ad(String id, String title, Car car, List<URI> carPhotos, URI link) {
        this.id = id;
        this.title = title;
        this.car = car;
        this.carPhotos = unmodifiableList(carPhotos);
        this.link = link;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ad)) return false;
        Ad ad = (Ad) o;
        return Objects.equals(id, ad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
