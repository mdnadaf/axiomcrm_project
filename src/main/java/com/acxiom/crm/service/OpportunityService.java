package com.acxiom.crm.service;

import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.viewmodel.OpportunityForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OpportunityService {

    Page<Opportunity> listOpportunities(User scopeUser, String search, OpportunityStage stage, OpportunityStatus status, Pageable pageable);

    Opportunity getOpportunity(Long id, User scopeUser);

    Opportunity createOpportunity(OpportunityForm form, CrmUserDetailsService.CrmUserPrincipal principal);

    Opportunity updateOpportunity(Long id, OpportunityForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    void deleteOpportunity(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    OpportunityForm toForm(Opportunity opportunity);

    List<Opportunity> getPipeline(User scopeUser);
}
