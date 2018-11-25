package com.zzz3bra.silviculture.data;

import com.zzz3bra.silviculture.data.gathering.Search;
import io.ebean.Model;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

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

    @OneToMany(cascade = CascadeType.ALL)
    private List<Search> searches;

}
