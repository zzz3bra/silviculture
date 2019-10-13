package com.zzz3bra.silviculture.ports.out;

import com.zzz3bra.silviculture.domain.Customer;

import java.util.Collection;
import java.util.Optional;

public interface LoadCustomerPort {

    Optional<Customer> loadOneCustomersById(Long id);

    Collection<Customer> loadAllCustomers();
}
