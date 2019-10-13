package com.zzz3bra.silviculture.domain;

import io.ebean.Model;
import io.ebean.annotation.DbJsonB;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Customer extends Model {
    @Id
    private long id;

    private String name;

    @DbJsonB
    private List<Search> searches;

    @DbJsonB
    private Map<String, Set<String>> viewedAdsIdsBySearcher;

}
