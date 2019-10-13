package com.zzz3bra.silviculture.adapter.out.persistence;

import com.zzz3bra.silviculture.domain.Customer;
import com.zzz3bra.silviculture.ports.out.LoadCustomerPort;

import java.util.Collection;
import java.util.Optional;

public class JpaCustomerPersistenceAdapter implements LoadCustomerPort {
    @Override
    public Optional<Customer> loadOneCustomersById(Long id) {
        return Optional.ofNullable(CustomerFinder.INSTANCE.byId(id));
    }

    @Override
    public Collection<Customer> loadAllCustomers() {
        return CustomerFinder.INSTANCE.all();
    }
}
