package com.zzz3bra.silviculture.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Customer {

    @Id
    private long id;

    private String name;

}
