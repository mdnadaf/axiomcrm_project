package com.acxiom.crm.controller;

import com.acxiom.crm.enums.ActivityStatus;
import com.acxiom.crm.enums.ActivityType;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.ActivityService;
import com.acxiom.crm.service.CustomerService;
import com.acxiom.crm.service.LeadService;
import com.acxiom.crm.service.UserService;
import com.acxiom.crm.viewmodel.ActivityForm;
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

import java.time.LocalDateTime;

@Controller
@RequestMapping("/activities")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final CustomerService customerService;
    private final LeadService leadService;
    private final UserService userService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    public String list(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(defaultValue = "activityDate") String sort,
                       @RequestParam(defaultValue = "DESC") String direction,
                       @RequestParam(required = false) ActivityType type,
                       @RequestParam(required = false) ActivityStatus status,
                       Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        var activities = activityService.listActivities(scopeUser, type, status, null, null, pageable);
        model.addAttribute("activities", activities);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("types", ActivityType.values());
        model.addAttribute("statuses", ActivityStatus.values());
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        return "activity/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        ActivityForm form = new ActivityForm();
        form.setActivityDate(LocalDateTime.now());
        form.setType("CALL");
        form.setStatus("PENDING");
        model.addAttribute("activityForm", form);
        populateDropdowns(principal, model);
        return "activity/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("activityForm") ActivityForm activityForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            populateDropdowns(principal, model);
            return "activity/form";
        }
        var act = activityService.createActivity(activityForm, principal);
        redirectAttributes.addFlashAttribute("successMessage", "Activity '" + act.getSubject() + "' created successfully.");
        return "redirect:/activities/" + act.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         Model model) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            var act = activityService.getActivity(id, scopeUser);
            model.addAttribute("activity", act);
            return "activity/detail";
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
            var act = activityService.getActivity(id, scopeUser);
            model.addAttribute("activityForm", activityService.toForm(act));
            model.addAttribute("activityId", id);
            populateDropdowns(principal, model);
            return "activity/form";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("activityForm") ActivityForm activityForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activityId", id);
            populateDropdowns(principal, model);
            return "activity/form";
        }
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            activityService.updateActivity(id, activityForm, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Activity updated successfully.");
            return "redirect:/activities/" + id;
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
            activityService.deleteActivity(id, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Activity deleted successfully.");
            return "redirect:/activities";
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
        model.addAttribute("types", ActivityType.values());
        model.addAttribute("statuses", ActivityStatus.values());
        if (scopeHelper.canAdminOrManager(principal)) {
            model.addAttribute("salesUsers", userService.listSalesExecutives());
        }
    }
}
