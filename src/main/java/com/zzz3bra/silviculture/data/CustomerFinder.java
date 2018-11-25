package com.zzz3bra.silviculture.data;

import io.ebean.Finder;

public class CustomerFinder extends Finder<Long, Customer> {
    public CustomerFinder() {
        super(Customer.class);
    }
}
