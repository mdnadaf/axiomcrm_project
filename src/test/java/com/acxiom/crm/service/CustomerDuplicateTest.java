package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.exception.DuplicateResourceException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService.CrmUserPrincipal;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.impl.CustomerServiceImpl;
import com.acxiom.crm.viewmodel.CustomerForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerDuplicateTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ScopeHelper scopeHelper;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CrmUserPrincipal principal;

    @BeforeEach
    void setUp() {
        User salesUser = User.builder()
                .id(1L)
                .email("sales1@acxiomcrm.com")
                .fullName("Sales One")
                .role(Role.ROLE_SALES_EXECUTIVE)
                .active(true)
                .build();
        principal = new CrmUserPrincipal(salesUser);
    }

    @Test
    void testDuplicateEmailThrowsDuplicateResourceException() {
        CustomerForm form = new CustomerForm();
        form.setName("Customer Alpha");
        form.setEmail("dup@example.com");
        form.setPhone("9876543210");
        form.setCompany("Acme Corp");

        when(customerRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            customerService.createCustomer(form, principal);
        });
    }

    @Test
    void testDuplicatePhoneThrowsDuplicateResourceException() {
        CustomerForm form = new CustomerForm();
        form.setName("Customer Alpha");
        form.setEmail("unique@example.com");
        form.setPhone("9876543210");
        form.setCompany("Acme Corp");

        when(customerRepository.existsByEmail("unique@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("9876543210")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            customerService.createCustomer(form, principal);
        });
    }

    @Test
    void testCreateCustomerGeneratesSequentialCode() {
        CustomerForm form = new CustomerForm();
        form.setName("Customer Alpha");
        form.setEmail("new@example.com");
        form.setPhone("9876543210");
        form.setCompany("Acme Corp");

        when(customerRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(customerRepository.existsByPhone("9876543210")).thenReturn(false);
        when(customerRepository.findMaxCustomerCodeSequence()).thenReturn(5);
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        Customer created = customerService.createCustomer(form, principal);
        assertNotNull(created);
        assertEquals("CUST-0006", created.getCustomerCode());
        assertEquals("Customer Alpha", created.getName());
    }
}
