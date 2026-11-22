package com.acxiom.crm.controller;

import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.CustomerService;
import com.acxiom.crm.service.LeadService;
import com.acxiom.crm.service.OpportunityService;
import com.acxiom.crm.service.UserService;
import com.acxiom.crm.viewmodel.OpportunityForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/opportunities")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final CustomerService customerService;
    private final LeadService leadService;
    private final UserService userService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    public String list(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(defaultValue = "createdDate") String sort,
                       @RequestParam(defaultValue = "DESC") String direction,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) OpportunityStage stage,
                       @RequestParam(required = false) OpportunityStatus status,
                       Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        var opps = opportunityService.listOpportunities(scopeUser, search, stage, status, pageable);
        model.addAttribute("opportunities", opps);
        model.addAttribute("search", search);
        model.addAttribute("selectedStage", stage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("stages", OpportunityStage.values());
        model.addAttribute("statuses", OpportunityStatus.values());
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        return "opportunity/list";
    }

    @GetMapping("/pipeline")
    public String pipeline(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pipeline = opportunityService.getPipeline(scopeUser);
        model.addAttribute("opportunities", pipeline);
        model.addAttribute("stages", OpportunityStage.values());
        return "opportunity/pipeline";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        OpportunityForm form = new OpportunityForm();
        form.setStage("QUALIFICATION");
        form.setProbability(20);
        model.addAttribute("opportunityForm", form);
        populateDropdowns(principal, model);
        return "opportunity/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("opportunityForm") OpportunityForm opportunityForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            populateDropdowns(principal, model);
            return "opportunity/form";
        }
        try {
            var opp = opportunityService.createOpportunity(opportunityForm, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Opportunity '" + opp.getName() + "' created successfully.");
            return "redirect:/opportunities/" + opp.getId();
        } catch (BusinessException ex) {
            bindingResult.reject("error.opportunityForm", ex.getMessage());
            populateDropdowns(principal, model);
            return "opportunity/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         Model model) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            var opp = opportunityService.getOpportunity(id, scopeUser);
            model.addAttribute("opportunity", opp);
            return "opportunity/detail";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                           Model model) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            var opp = opportunityService.getOpportunity(id, scopeUser);
            model.addAttribute("opportunityForm", opportunityService.toForm(opp));
            model.addAttribute("opportunityId", id);
            populateDropdowns(principal, model);
            return "opportunity/form";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("opportunityForm") OpportunityForm opportunityForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("opportunityId", id);
            populateDropdowns(principal, model);
            return "opportunity/form";
        }
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            opportunityService.updateOpportunity(id, opportunityForm, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Opportunity updated successfully.");
            return "redirect:/opportunities/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        } catch (BusinessException ex) {
            bindingResult.reject("error.opportunityForm", ex.getMessage());
            model.addAttribute("opportunityId", id);
            populateDropdowns(principal, model);
            return "opportunity/form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            opportunityService.deleteOpportunity(id, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Opportunity deleted successfully.");
            return "redirect:/opportunities";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    private void populateDropdowns(CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        model.addAttribute("customers", customerService.listAllActive(scopeUser));
        model.addAttribute("leads", leadService.listAllActive(scopeUser));
        model.addAttribute("stages", OpportunityStage.values());
        if (scopeHelper.canAdminOrManager(principal)) {
            model.addAttribute("salesUsers", userService.listSalesExecutives());
        }
    }
}
