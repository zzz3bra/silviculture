package com.zzz3bra.silviculture.ports.in;

import com.zzz3bra.silviculture.domain.Customer;

public interface RemoveAllCarSearchesUseCase {
    void removeAllCarSearchesForCustomer(Customer customer);
}
