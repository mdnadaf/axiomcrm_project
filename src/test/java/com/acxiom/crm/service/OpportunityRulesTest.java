package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.OpportunityRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.service.impl.OpportunityServiceImpl;
import com.acxiom.crm.viewmodel.OpportunityForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OpportunityRulesTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private OpportunityServiceImpl opportunityService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id(1L)
                .name("Test Customer")
                .email("cust@test.com")
                .phone("9876543210")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Opportunity Amount must be greater than 0")
    void testOpportunityAmountMustBeGreaterThanZero() {
        OpportunityForm form = new OpportunityForm();
        form.setName("Zero Amount Deal");
        form.setCustomerId(1L);
        form.setStage("QUALIFICATION");
        form.setAmount(BigDecimal.ZERO);
        form.setProbability(50);
        form.setExpectedCloseDate(LocalDate.now().plusDays(10));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                opportunityService.createOpportunity(form, null));

        assertEquals("Opportunity Amount must be greater than 0.", ex.getMessage());
    }

    @Test
    @DisplayName("Opportunity Probability must not exceed 100")
    void testProbabilityMustNotExceed100() {
        OpportunityForm form = new OpportunityForm();
        form.setName("High Probability Deal");
        form.setCustomerId(1L);
        form.setStage("QUALIFICATION");
        form.setAmount(BigDecimal.valueOf(1000));
        form.setProbability(105);
        form.setExpectedCloseDate(LocalDate.now().plusDays(10));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                opportunityService.createOpportunity(form, null));

        assertEquals("Probability must be between 0 and 100.", ex.getMessage());
    }

    @Test
    @DisplayName("Opportunity Probability must not be negative")
    void testProbabilityMustNotBeNegative() {
        OpportunityForm form = new OpportunityForm();
        form.setName("Negative Probability Deal");
        form.setCustomerId(1L);
        form.setStage("QUALIFICATION");
        form.setAmount(BigDecimal.valueOf(1000));
        form.setProbability(-10);
        form.setExpectedCloseDate(LocalDate.now().plusDays(10));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                opportunityService.createOpportunity(form, null));

        assertEquals("Probability must be between 0 and 100.", ex.getMessage());
    }

    @Test
    @DisplayName("Expected Close Date cannot be in the past for active opportunity")
    void testExpectedCloseDateCannotBeInPastForActive() {
        OpportunityForm form = new OpportunityForm();
        form.setName("Past Close Date Deal");
        form.setCustomerId(1L);
        form.setStage("PROPOSAL"); // active stage
        form.setAmount(BigDecimal.valueOf(5000));
        form.setProbability(50);
        form.setExpectedCloseDate(LocalDate.now().minusDays(1)); // yesterday!

        BusinessException ex = assertThrows(BusinessException.class, () ->
                opportunityService.createOpportunity(form, null));

        assertEquals("Expected Close Date cannot be in the past.", ex.getMessage());
    }

    @Test
    @DisplayName("Valid Opportunity is saved successfully and weighted value calculated")
    void testValidOpportunityCreation() {
        OpportunityForm form = new OpportunityForm();
        form.setName("Valid Deal");
        form.setCustomerId(1L);
        form.setStage("QUALIFICATION");
        form.setAmount(BigDecimal.valueOf(10000));
        form.setProbability(50);
        form.setExpectedCloseDate(LocalDate.now().plusMonths(1));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> {
            Opportunity o = inv.getArgument(0);
            o.setId(10L);
            return o;
        });

        Opportunity created = opportunityService.createOpportunity(form, null);
        assertNotNull(created);
        assertEquals("Valid Deal", created.getName());
        assertEquals(BigDecimal.valueOf(10000), created.getAmount());
        assertEquals(50, created.getProbability());
        assertEquals(new BigDecimal("5000.00"), created.getWeightedValue());
        verify(opportunityRepository, times(1)).save(any(Opportunity.class));
    }
}
