package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.LeadStatus;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.OpportunityRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService.CrmUserPrincipal;
import com.acxiom.crm.service.impl.LeadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeadConversionTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LeadServiceImpl leadService;

    private User salesUser;
    private CrmUserPrincipal principal;
    private Lead qualifiedLead;

    @BeforeEach
    void setUp() {
        salesUser = User.builder()
                .id(1L)
                .email("sales1@acxiomcrm.com")
                .fullName("Sales One")
                .role(Role.ROLE_SALES_EXECUTIVE)
                .active(true)
                .build();
        principal = new CrmUserPrincipal(salesUser);

        qualifiedLead = Lead.builder()
                .id(200L)
                .leadCode("LEAD-0005")
                .name("Alice Smith")
                .company("Smith Enterprises")
                .email("alice@smith.com")
                .phone("9876543211")
                .status(LeadStatus.QUALIFIED)
                .assignedTo(salesUser)
                .expectedValue(BigDecimal.valueOf(75000))
                .build();
    }

    @Test
    void testConvertQualifiedLeadCreatesCustomerAndOpportunity() {
        when(leadRepository.findById(200L)).thenReturn(Optional.of(qualifiedLead));
        when(customerRepository.findByEmail("alice@smith.com")).thenReturn(Optional.empty());
        when(customerRepository.findMaxCustomerCodeSequence()).thenReturn(3);

        Customer savedCust = Customer.builder()
                .id(15L)
                .customerCode("CUST-0004")
                .name("Alice Smith")
                .email("alice@smith.com")
                .phone("9876543211")
                .company("Smith Enterprises")
                .assignedTo(salesUser)
                .build();
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCust);

        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(i -> {
            Opportunity opp = i.getArgument(0);
            opp.setId(50L);
            return opp;
        });
        when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));

        Customer result = leadService.convertLead(200L, salesUser, principal);
        assertNotNull(result);

        // Verify Customer was created with proper fields
        ArgumentCaptor<Customer> custCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(custCaptor.capture());
        Customer createdCustomer = custCaptor.getValue();
        assertEquals("Alice Smith", createdCustomer.getName());
        assertEquals("alice@smith.com", createdCustomer.getEmail());
        assertEquals("Smith Enterprises", createdCustomer.getCompany());

        // Verify Opportunity was created in QUALIFICATION stage with amount = expectedValue
        ArgumentCaptor<Opportunity> oppCaptor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(oppCaptor.capture());
        Opportunity createdOpp = oppCaptor.getValue();
        assertEquals(OpportunityStage.QUALIFICATION, createdOpp.getStage());
        assertEquals(OpportunityStatus.OPEN, createdOpp.getStatus());
        assertEquals(BigDecimal.valueOf(75000), createdOpp.getAmount());
        assertEquals(savedCust, createdOpp.getCustomer());
        assertEquals(qualifiedLead, createdOpp.getLead());

        // Verify lead status is CONVERTED
        assertEquals(LeadStatus.CONVERTED, qualifiedLead.getStatus());

        // Verify audit log was recorded
        verify(auditLogService, times(1)).logCustom(
                eq("CONVERT_LEAD"), eq("Lead"), eq(200L), any(), any()
        );
    }

    @Test
    void testConvertNonQualifiedLeadThrowsException() {
        qualifiedLead.setStatus(LeadStatus.NEW);
        when(leadRepository.findById(200L)).thenReturn(Optional.of(qualifiedLead));

        assertThrows(IllegalStateException.class, () -> {
            leadService.convertLead(200L, salesUser, principal);
        });
    }

    @Test
    void testConvertLeadReusesExistingCustomerIfEmailMatches() {
        when(leadRepository.findById(200L)).thenReturn(Optional.of(qualifiedLead));

        Customer existingCust = Customer.builder()
                .id(12L)
                .customerCode("CUST-0002")
                .name("Alice Smith Existing")
                .email("alice@smith.com")
                .phone("9876543211")
                .company("Smith Corp")
                .assignedTo(salesUser)
                .build();
        when(customerRepository.findByEmail("alice@smith.com")).thenReturn(Optional.of(existingCust));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(i -> i.getArgument(0));
        when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));

        leadService.convertLead(200L, salesUser, principal);

        // Verify customerRepository.save was NOT called since customer existed
        verify(customerRepository, never()).save(any(Customer.class));

        // Verify opportunity references existing customer
        ArgumentCaptor<Opportunity> oppCaptor = ArgumentCaptor.forClass(Opportunity.class);
        verify(opportunityRepository).save(oppCaptor.capture());
        assertEquals(existingCust, oppCaptor.getValue().getCustomer());
    }
}
