package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.viewmodel.LeadForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LeadService {

    Page<Lead> listLeads(User scopeUser, String search, String status, Pageable pageable);

    Lead getLead(Long id, User scopeUser);

    Lead createLead(LeadForm form, CrmUserDetailsService.CrmUserPrincipal principal);

    Lead updateLead(Long id, LeadForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    void deleteLead(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    Customer convertLead(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    Lead changeStatus(Long id, String newStatus, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal);

    LeadForm toForm(Lead lead);

    List<Lead> listAllActive(User scopeUser);
}
