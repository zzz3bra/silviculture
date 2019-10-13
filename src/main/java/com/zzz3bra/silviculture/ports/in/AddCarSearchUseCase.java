package com.zzz3bra.silviculture.ports.in;

import com.zzz3bra.silviculture.domain.Customer;
import com.zzz3bra.silviculture.domain.Search;

public interface AddCarSearchUseCase {
    void addSearchToCustomer(Customer customer, Search search);
}
