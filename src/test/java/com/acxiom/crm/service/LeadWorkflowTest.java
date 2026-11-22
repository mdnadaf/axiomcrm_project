package com.acxiom.crm.service;

import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.LeadStatus;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.exception.InvalidLeadStatusTransitionException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.OpportunityRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService.CrmUserPrincipal;
import com.acxiom.crm.service.impl.LeadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeadWorkflowTest {

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
    private Lead lead;

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

        lead = Lead.builder()
                .id(100L)
                .leadCode("LEAD-0001")
                .name("John Doe")
                .company("Acme Ltd")
                .email("john.doe@acme.com")
                .phone("9876543210")
                .status(LeadStatus.NEW)
                .assignedTo(salesUser)
                .expectedValue(BigDecimal.valueOf(50000))
                .build();
    }

    @Test
    void testValidTransitionNewToContacted() {
        when(leadRepository.findById(100L)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));

        Lead updated = leadService.changeStatus(100L, "CONTACTED", salesUser, principal);
        assertEquals(LeadStatus.CONTACTED, updated.getStatus());
    }

    @Test
    void testValidTransitionContactedToQualified() {
        lead.setStatus(LeadStatus.CONTACTED);
        when(leadRepository.findById(100L)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));

        Lead updated = leadService.changeStatus(100L, "QUALIFIED", salesUser, principal);
        assertEquals(LeadStatus.QUALIFIED, updated.getStatus());
    }

    @Test
    void testInvalidTransitionNewToQualifiedFails() {
        when(leadRepository.findById(100L)).thenReturn(Optional.of(lead));

        assertThrows(InvalidLeadStatusTransitionException.class, () -> {
            leadService.changeStatus(100L, "QUALIFIED", salesUser, principal);
        });
    }

    @Test
    void testInvalidTransitionNewToConvertedFails() {
        when(leadRepository.findById(100L)).thenReturn(Optional.of(lead));

        assertThrows(InvalidLeadStatusTransitionException.class, () -> {
            leadService.changeStatus(100L, "CONVERTED", salesUser, principal);
        });
    }

    @Test
    void testFinalStatusConvertedCannotBeChanged() {
        lead.setStatus(LeadStatus.CONVERTED);
        when(leadRepository.findById(100L)).thenReturn(Optional.of(lead));

        assertThrows(InvalidLeadStatusTransitionException.class, () -> {
            leadService.changeStatus(100L, "CONTACTED", salesUser, principal);
        });
    }

    @Test
    void testFinalStatusLostCannotBeChanged() {
        lead.setStatus(LeadStatus.LOST);
        when(leadRepository.findById(100L)).thenReturn(Optional.of(lead));

        assertThrows(InvalidLeadStatusTransitionException.class, () -> {
            leadService.changeStatus(100L, "NEW", salesUser, principal);
        });
    }
}
