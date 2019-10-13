package com.zzz3bra.silviculture.application;

import com.zzz3bra.silviculture.domain.Customer;
import com.zzz3bra.silviculture.domain.Search;
import com.zzz3bra.silviculture.ports.in.AddCarSearchUseCase;
import com.zzz3bra.silviculture.ports.in.DeleteCarSearchUseCase;
import com.zzz3bra.silviculture.ports.in.RemoveAllCarSearchesUseCase;
import com.zzz3bra.silviculture.ports.in.ResetViewedCarSearchesUseCase;
import com.zzz3bra.silviculture.ports.out.LoadCustomerPort;

public class MainService implements AddCarSearchUseCase, DeleteCarSearchUseCase, RemoveAllCarSearchesUseCase, ResetViewedCarSearchesUseCase {

    private LoadCustomerPort loadCustomerPort;

    @Override
    public void addSearchToCustomer(Customer customer, Search search) {

    }

    @Override
    public void deleteCustomer(Customer customer) {
        customer.delete();
    }

    @Override
    public void removeAllCarSearchesForCustomer(Customer customer) {
        customer.getSearches().clear();
    }

    @Override
    public void resetViewedAdsOfCustomer(Customer customer) {
        customer.getViewedAdsIdsBySearcher().clear();
    }
}
