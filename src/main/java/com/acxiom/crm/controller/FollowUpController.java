package com.acxiom.crm.controller;

import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.enums.FollowUpType;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.CustomerService;
import com.acxiom.crm.service.FollowUpService;
import com.acxiom.crm.service.LeadService;
import com.acxiom.crm.service.UserService;
import com.acxiom.crm.viewmodel.FollowUpForm;
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

import java.time.LocalDate;

@Controller
@RequestMapping("/followups")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;
    private final CustomerService customerService;
    private final LeadService leadService;
    private final UserService userService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    public String list(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(defaultValue = "followUpDate") String sort,
                       @RequestParam(defaultValue = "ASC") String direction,
                       @RequestParam(required = false) FollowUpStatus status,
                       @RequestParam(required = false) LocalDate fromDate,
                       @RequestParam(required = false) LocalDate toDate,
                       Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        var followups = followUpService.listFollowUps(scopeUser, status, fromDate, toDate, pageable);
        model.addAttribute("followups", followups);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", FollowUpStatus.values());
        model.addAttribute("types", FollowUpType.values());
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        return "followup/list";
    }

    @GetMapping("/pending")
    public String pending(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "followUpDate"));
        var followups = followUpService.getPendingFollowUps(scopeUser, pageable);
        model.addAttribute("followups", followups);
        model.addAttribute("title", "Pending Follow-Ups");
        return "followup/pending";
    }

    @GetMapping("/overdue")
    public String overdue(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "followUpDate"));
        var followups = followUpService.getOverdueFollowUps(scopeUser, pageable);
        model.addAttribute("followups", followups);
        model.addAttribute("title", "Overdue / Missed Follow-Ups");
        return "followup/overdue";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        FollowUpForm form = new FollowUpForm();
        form.setFollowUpDate(LocalDate.now());
        form.setType("CALL");
        form.setStatus("PLANNED");
        model.addAttribute("followUpForm", form);
        populateDropdowns(principal, model);
        return "followup/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("followUpForm") FollowUpForm followUpForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            populateDropdowns(principal, model);
            return "followup/form";
        }
        try {
            var fu = followUpService.createFollowUp(followUpForm, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Follow-up scheduled successfully.");
            return "redirect:/followups/" + fu.getId();
        } catch (BusinessException ex) {
            bindingResult.reject("error.followUpForm", ex.getMessage());
            populateDropdowns(principal, model);
            return "followup/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         Model model) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            var fu = followUpService.getFollowUp(id, scopeUser);
            model.addAttribute("followUp", fu);
            return "followup/detail";
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
            var fu = followUpService.getFollowUp(id, scopeUser);
            model.addAttribute("followUpForm", followUpService.toForm(fu));
            model.addAttribute("followUpId", id);
            populateDropdowns(principal, model);
            return "followup/form";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("followUpForm") FollowUpForm followUpForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("followUpId", id);
            populateDropdowns(principal, model);
            return "followup/form";
        }
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            followUpService.updateFollowUp(id, followUpForm, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Follow-up updated successfully.");
            return "redirect:/followups/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        } catch (BusinessException ex) {
            bindingResult.reject("error.followUpForm", ex.getMessage());
            model.addAttribute("followUpId", id);
            populateDropdowns(principal, model);
            return "followup/form";
        }
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                           @RequestParam(required = false) String completionNotes,
                           @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            followUpService.completeFollowUp(id, completionNotes, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Follow-up marked as COMPLETED.");
            return "redirect:/followups/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @RequestParam LocalDate newDate,
                             @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            followUpService.rescheduleFollowUp(id, newDate, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Follow-up rescheduled to " + newDate + ".");
            return "redirect:/followups/" + id;
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/followups/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            followUpService.cancelFollowUp(id, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Follow-up marked as CANCELLED.");
            return "redirect:/followups/" + id;
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
            followUpService.deleteFollowUp(id, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Follow-up deleted successfully.");
            return "redirect:/followups";
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
        model.addAttribute("types", FollowUpType.values());
        model.addAttribute("statuses", FollowUpStatus.values());
        if (scopeHelper.canAdminOrManager(principal)) {
            model.addAttribute("salesUsers", userService.listSalesExecutives());
        }
    }
}
