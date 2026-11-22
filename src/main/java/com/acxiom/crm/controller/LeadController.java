package com.acxiom.crm.controller;

import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.CustomerService;
import com.acxiom.crm.service.LeadService;
import com.acxiom.crm.service.UserService;
import com.acxiom.crm.viewmodel.LeadForm;
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
@RequestMapping("/leads")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final CustomerService customerService;
    private final UserService userService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    public String list(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(defaultValue = "createdDate") String sort,
                       @RequestParam(defaultValue = "DESC") String direction,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String status,
                       Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        var leads = leadService.listLeads(scopeUser, search, status, pageable);
        model.addAttribute("leads", leads);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        return "lead/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        LeadForm form = new LeadForm();
        form.setStatus("NEW");
        model.addAttribute("leadForm", form);
        var scopeUser = scopeHelper.getScopeUser(principal);
        model.addAttribute("customers", customerService.listAllActive(scopeUser));
        if (scopeHelper.canAdminOrManager(principal)) {
            model.addAttribute("salesUsers", userService.listSalesExecutives());
        }
        return "lead/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("leadForm") LeadForm leadForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            var scopeUser = scopeHelper.getScopeUser(principal);
            model.addAttribute("customers", customerService.listAllActive(scopeUser));
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "lead/form";
        }
        try {
            var lead = leadService.createLead(leadForm, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Lead '" + lead.getName() + "' created successfully.");
            return "redirect:/leads/" + lead.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "error.leadForm", ex.getMessage());
            var scopeUser = scopeHelper.getScopeUser(principal);
            model.addAttribute("customers", customerService.listAllActive(scopeUser));
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "lead/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         Model model) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            var lead = leadService.getLead(id, scopeUser);
            model.addAttribute("lead", lead);
            return "lead/detail";
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
            var lead = leadService.getLead(id, scopeUser);
            var form = leadService.toForm(lead);
            model.addAttribute("leadForm", form);
            model.addAttribute("leadId", id);
            model.addAttribute("customers", customerService.listAllActive(scopeUser));
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "lead/form";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("leadForm") LeadForm leadForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("leadId", id);
            var scopeUser = scopeHelper.getScopeUser(principal);
            model.addAttribute("customers", customerService.listAllActive(scopeUser));
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "lead/form";
        }
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            leadService.updateLead(id, leadForm, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Lead updated successfully.");
            return "redirect:/leads/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            leadService.deleteLead(id, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Lead deleted successfully.");
            return "redirect:/leads";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/convert")
    public String convert(@PathVariable Long id,
                          @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            var customer = leadService.convertLead(id, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Lead converted successfully.");
            return "redirect:/customers/" + customer.getId();
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/leads/" + id;
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                               @RequestParam String newStatus,
                               RedirectAttributes redirectAttributes) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            leadService.changeStatus(id, newStatus, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Lead status updated to " + newStatus + ".");
            return "redirect:/leads/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/leads/" + id;
        }
    }
}
