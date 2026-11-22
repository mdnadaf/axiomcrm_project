package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.exception.DuplicateResourceException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.service.CustomerService;
import com.acxiom.crm.viewmodel.CustomerForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> listCustomers(User scopeUser, String search, Boolean active, Pageable pageable) {
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        return customerRepository.search(searchTerm, scopeUser, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomer(Long id, User scopeUser) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        validateScope(customer, scopeUser);
        return customer;
    }

    @Override
    @Transactional
    public Customer createCustomer(CustomerForm form, CrmUserDetailsService.CrmUserPrincipal principal) {
        if (customerRepository.existsByEmail(form.getEmail())) {
            throw new DuplicateResourceException("Customer with email '" + form.getEmail() + "' already exists.");
        }
        if (customerRepository.existsByPhone(form.getPhone())) {
            throw new DuplicateResourceException("Customer with phone '" + form.getPhone() + "' already exists.");
        }

        User assignedTo = null;
        if (form.getAssignedToId() != null) {
            assignedTo = userRepository.findById(form.getAssignedToId()).orElse(null);
        }
        if (assignedTo == null && principal != null) {
            assignedTo = principal.getUser();
        }

        User createdBy = principal != null ? principal.getUser() : null;

        int nextSeq = customerRepository.findMaxCustomerCodeSequence() + 1;
        String customerCode = String.format("CUST-%04d", nextSeq);

        Customer customer = Customer.builder()
                .customerCode(customerCode)
                .name(form.getName().trim())
                .email(form.getEmail().trim().toLowerCase())
                .phone(form.getPhone().trim())
                .company(form.getCompany())
                .address(form.getAddress())
                .notes(form.getNotes())
                .active(form.isActive())
                .assignedTo(assignedTo)
                .createdBy(createdBy)
                .build();

        customer = customerRepository.save(customer);
        auditLogService.logCreate("Customer", customer.getId(), customer.getName());
        return customer;
    }

    @Override
    @Transactional
    public Customer updateCustomer(Long id, CustomerForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Customer customer = getCustomer(id, scopeUser);

        if (customerRepository.existsByEmailAndIdNot(form.getEmail(), id)) {
            throw new DuplicateResourceException("Customer with email '" + form.getEmail() + "' already exists.");
        }
        if (customerRepository.existsByPhoneAndIdNot(form.getPhone(), id)) {
            throw new DuplicateResourceException("Customer with phone '" + form.getPhone() + "' already exists.");
        }

        String oldSnapshot = customer.getName() + " (" + customer.getEmail() + ")";

        customer.setName(form.getName().trim());
        customer.setEmail(form.getEmail().trim().toLowerCase());
        customer.setPhone(form.getPhone().trim());
        customer.setCompany(form.getCompany());
        customer.setAddress(form.getAddress());
        customer.setNotes(form.getNotes());
        customer.setActive(form.isActive());

        if (form.getAssignedToId() != null) {
            userRepository.findById(form.getAssignedToId()).ifPresent(customer::setAssignedTo);
        }

        customer = customerRepository.save(customer);
        auditLogService.logUpdate("Customer", customer.getId(), oldSnapshot, customer.getName() + " (" + customer.getEmail() + ")");
        return customer;
    }

    @Override
    @Transactional
    public void deactivateCustomer(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Customer customer = getCustomer(id, scopeUser);
        customer.setActive(false);
        customerRepository.save(customer);
        auditLogService.logDelete("Customer", customer.getId(), "Deactivated customer " + customer.getName());
    }

    @Override
    public CustomerForm toForm(Customer customer) {
        CustomerForm form = new CustomerForm();
        form.setId(customer.getId());
        form.setName(customer.getName());
        form.setEmail(customer.getEmail());
        form.setPhone(customer.getPhone());
        form.setCompany(customer.getCompany());
        form.setAddress(customer.getAddress());
        form.setNotes(customer.getNotes());
        form.setActive(customer.isActive());
        if (customer.getAssignedTo() != null) {
            form.setAssignedToId(customer.getAssignedTo().getId());
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer findByEmail(String email) {
        return customerRepository.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return customerRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> listAllActive(User scopeUser) {
        if (scopeUser == null) {
            return customerRepository.findAll().stream().filter(Customer::isActive).toList();
        }
        return customerRepository.findAll().stream()
                .filter(c -> c.isActive() && c.getAssignedTo() != null && c.getAssignedTo().getId().equals(scopeUser.getId()))
                .toList();
    }

    private void validateScope(Customer customer, User scopeUser) {
        if (scopeUser == null) return; // Admin / Manager has full access
        boolean isAssigned = customer.getAssignedTo() != null && customer.getAssignedTo().getId().equals(scopeUser.getId());
        boolean isCreated = customer.getCreatedBy() != null && customer.getCreatedBy().getId().equals(scopeUser.getId());
        if (!isAssigned && !isCreated) {
            throw new AccessDeniedException("Access denied: You are not authorized to access customer ID " + customer.getId());
        }
    }
}
