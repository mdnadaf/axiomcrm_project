package com.acxiom.crm.service;

import com.acxiom.crm.entity.FollowUp;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.viewmodel.FollowUpForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface FollowUpService {

    Page<FollowUp> listFollowUps(User scopeUser, FollowUpStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    FollowUp getFollowUp(Long id, User scopeUser);

    FollowUp createFollowUp(FollowUpForm form, CrmUserDetailsService.CrmUserPrincipal principal);

    FollowUp updateFollowUp(Long id, FollowUpForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    FollowUp completeFollowUp(Long id, String completionNotes, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    FollowUp rescheduleFollowUp(Long id, LocalDate newDate, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    FollowUp cancelFollowUp(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    void deleteFollowUp(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    FollowUpForm toForm(FollowUp followUp);

    Page<FollowUp> getPendingFollowUps(User scopeUser, Pageable pageable);

    Page<FollowUp> getOverdueFollowUps(User scopeUser, Pageable pageable);

    int markOverdueAsMissed();
}
