package com.zzz3bra.silviculture.data;

import com.zzz3bra.silviculture.data.gathering.Search;
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

    public static final CustomerFinder find = new CustomerFinder();

    @Id
    private long id;

    private String name;

    @DbJsonB
    private List<Search> searches;

    @DbJsonB
    private Map<String, Set<String>> viewedAdsIdsBySearcher;

}
