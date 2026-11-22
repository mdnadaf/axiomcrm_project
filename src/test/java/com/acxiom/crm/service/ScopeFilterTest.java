package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService.CrmUserPrincipal;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScopeFilterTest {

    private ScopeHelper scopeHelper;

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private User adminUser;
    private User managerUser;
    private User sales1;
    private User sales2;

    private CrmUserPrincipal adminPrincipal;
    private CrmUserPrincipal managerPrincipal;
    private CrmUserPrincipal sales1Principal;
    private CrmUserPrincipal sales2Principal;

    @BeforeEach
    void setUp() {
        scopeHelper = new ScopeHelper();

        adminUser = User.builder()
                .id(1L)
                .email("admin@acxiomcrm.com")
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();
        adminPrincipal = new CrmUserPrincipal(adminUser);

        managerUser = User.builder()
                .id(2L)
                .email("manager@acxiomcrm.com")
                .role(Role.ROLE_MANAGER)
                .active(true)
                .build();
        managerPrincipal = new CrmUserPrincipal(managerUser);

        sales1 = User.builder()
                .id(3L)
                .email("sales1@acxiomcrm.com")
                .role(Role.ROLE_SALES_EXECUTIVE)
                .active(true)
                .build();
        sales1Principal = new CrmUserPrincipal(sales1);

        sales2 = User.builder()
                .id(4L)
                .email("sales2@acxiomcrm.com")
                .role(Role.ROLE_SALES_EXECUTIVE)
                .active(true)
                .build();
        sales2Principal = new CrmUserPrincipal(sales2);
    }

    @Test
    void testScopeHelperRoles() {
        assertNull(scopeHelper.getScopeUser(adminPrincipal)); // Admin sees all
        assertNull(scopeHelper.getScopeUser(managerPrincipal)); // Manager sees all/team
        assertNotNull(scopeHelper.getScopeUser(sales1Principal)); // Sales sees only own
        assertEquals(sales1, scopeHelper.getScopeUser(sales1Principal));

        assertTrue(scopeHelper.isAdmin(adminPrincipal));
        assertTrue(scopeHelper.isManager(managerPrincipal));
        assertTrue(scopeHelper.isSalesExecutive(sales1Principal));
    }

    @Test
    void testAdminCanAccessAnyCustomer() {
        Customer customerOwnedBySales1 = Customer.builder()
                .id(10L)
                .assignedTo(sales1)
                .build();

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customerOwnedBySales1));

        // When scopeUser is null (admin), access is permitted
        Customer retrieved = customerService.getCustomer(10L, null);
        assertNotNull(retrieved);
        assertEquals(10L, retrieved.getId());
    }

    @Test
    void testSalesExecutiveCanAccessOwnRecord() {
        Customer customerOwnedBySales1 = Customer.builder()
                .id(10L)
                .assignedTo(sales1)
                .build();

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customerOwnedBySales1));

        Customer retrieved = customerService.getCustomer(10L, sales1);
        assertNotNull(retrieved);
        assertEquals(10L, retrieved.getId());
    }

    @Test
    void testSalesExecutiveCannotAccessOtherSalesRecord() {
        Customer customerOwnedBySales2 = Customer.builder()
                .id(20L)
                .assignedTo(sales2)
                .build();

        when(customerRepository.findById(20L)).thenReturn(Optional.of(customerOwnedBySales2));

        // Sales1 attempting to access Sales2's record gets AccessDeniedException (IDOR protection)
        assertThrows(AccessDeniedException.class, () -> {
            customerService.getCustomer(20L, sales1);
        });
    }
}
