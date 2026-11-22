package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.FollowUp;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.enums.FollowUpType;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.FollowUpRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService.CrmUserPrincipal;
import com.acxiom.crm.service.impl.FollowUpServiceImpl;
import com.acxiom.crm.viewmodel.FollowUpForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FollowUpRulesTest {

    @Mock
    private FollowUpRepository followUpRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FollowUpServiceImpl followUpService;

    private User salesUser;
    private CrmUserPrincipal principal;
    private Customer customer;

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

        customer = Customer.builder()
                .id(10L)
                .customerCode("CUST-0001")
                .name("Acme Corp")
                .email("acme@corp.com")
                .phone("9876543210")
                .assignedTo(salesUser)
                .build();
    }

    @Test
    void testFollowUpDateCannotBeEarlierThanTodayForNew() {
        FollowUpForm form = new FollowUpForm();
        form.setFollowUpDate(LocalDate.now().minusDays(1));
        form.setType("CALL");
        form.setStatus("PLANNED");
        form.setCustomerId(10L);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            followUpService.createFollowUp(form, principal);
        });

        assertEquals("Follow-up date cannot be earlier than today.", exception.getMessage());
    }

    @Test
    void testValidFollowUpDateSucceeds() {
        FollowUpForm form = new FollowUpForm();
        form.setFollowUpDate(LocalDate.now().plusDays(2));
        form.setType("CALL");
        form.setStatus("PLANNED");
        form.setCustomerId(10L);
        form.setRemarks("Check in call");

        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FollowUp created = followUpService.createFollowUp(form, principal);
        assertNotNull(created);
        assertEquals(FollowUpStatus.PLANNED, created.getStatus());
        assertEquals(form.getFollowUpDate(), created.getFollowUpDate());
    }

    @Test
    void testCompleteFollowUp() {
        FollowUp followUp = FollowUp.builder()
                .id(5L)
                .followUpDate(LocalDate.now())
                .type(FollowUpType.CALL)
                .status(FollowUpStatus.PLANNED)
                .customer(customer)
                .assignedTo(salesUser)
                .build();

        when(followUpRepository.findById(5L)).thenReturn(Optional.of(followUp));
        when(followUpRepository.save(any(FollowUp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        followUpService.completeFollowUp(5L, "Discussed pricing", salesUser, principal);

        assertEquals(FollowUpStatus.COMPLETED, followUp.getStatus());
        verify(auditLogService, times(1)).logCustom(
                eq("COMPLETE_FOLLOWUP"), eq("FollowUp"), eq(5L), eq("PLANNED"), any()
        );
    }

    @Test
    void testRescheduleFollowUpWithPastDateFails() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            followUpService.rescheduleFollowUp(5L, LocalDate.now().minusDays(1), salesUser, principal);
        });

        assertEquals("Follow-up date cannot be earlier than today.", exception.getMessage());
    }

    @Test
    void testMarkOverdueAsMissed() {
        FollowUp overdue = FollowUp.builder()
                .id(7L)
                .followUpDate(LocalDate.now().minusDays(2))
                .status(FollowUpStatus.PLANNED)
                .customer(customer)
                .assignedTo(salesUser)
                .build();

        when(followUpRepository.findOverduePlanned(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(overdue));

        int count = followUpService.markOverdueAsMissed();

        assertEquals(1, count);
        assertEquals(FollowUpStatus.MISSED, overdue.getStatus());
        verify(followUpRepository, times(1)).save(overdue);
    }
}
