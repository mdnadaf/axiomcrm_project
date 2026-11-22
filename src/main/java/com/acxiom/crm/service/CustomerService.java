package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.viewmodel.CustomerForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {

    Page<Customer> listCustomers(User scopeUser, String search, Boolean active, Pageable pageable);

    Customer getCustomer(Long id, User scopeUser);

    Customer createCustomer(CustomerForm form, CrmUserDetailsService.CrmUserPrincipal principal);

    Customer updateCustomer(Long id, CustomerForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    void deactivateCustomer(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    CustomerForm toForm(Customer customer);

    Customer findByEmail(String email);

    boolean existsByEmail(String email);

    List<Customer> listAllActive(User scopeUser);
}
