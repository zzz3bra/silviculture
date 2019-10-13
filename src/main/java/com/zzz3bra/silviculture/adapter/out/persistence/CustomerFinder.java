package com.zzz3bra.silviculture.adapter.out.persistence;

import com.zzz3bra.silviculture.domain.Customer;
import io.ebean.Finder;

class CustomerFinder extends Finder<Long, Customer> {
    static final CustomerFinder INSTANCE = new CustomerFinder();

    private CustomerFinder() {
        super(Customer.class);
    }
}
