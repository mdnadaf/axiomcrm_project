package com.acxiom.crm.controller;

import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.CustomerService;
import com.acxiom.crm.service.UserService;
import com.acxiom.crm.viewmodel.CustomerForm;
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
@RequestMapping("/customers")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CustomerController {

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
                       @RequestParam(required = false) Boolean active,
                       Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        var customers = customerService.listCustomers(scopeUser, search, active, pageable);
        model.addAttribute("customers", customers);
        model.addAttribute("search", search);
        model.addAttribute("active", active);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        return "customer/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        CustomerForm form = new CustomerForm();
        form.setActive(true);
        model.addAttribute("customerForm", form);
        if (scopeHelper.canAdminOrManager(principal)) {
            model.addAttribute("salesUsers", userService.listSalesExecutives());
        }
        return "customer/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("customerForm") CustomerForm customerForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "customer/form";
        }
        try {
            var customer = customerService.createCustomer(customerForm, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Customer '" + customer.getName() + "' created successfully.");
            return "redirect:/customers/" + customer.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "error.customerForm", ex.getMessage());
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "customer/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         Model model) {
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            var customer = customerService.getCustomer(id, scopeUser);
            model.addAttribute("customer", customer);
            return "customer/detail";
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
            var customer = customerService.getCustomer(id, scopeUser);
            var form = customerService.toForm(customer);
            model.addAttribute("customerForm", form);
            model.addAttribute("customerId", id);
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "customer/form";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("customerForm") CustomerForm customerForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customerId", id);
            if (scopeHelper.canAdminOrManager(principal)) {
                model.addAttribute("salesUsers", userService.listSalesExecutives());
            }
            return "customer/form";
        }
        try {
            var scopeUser = scopeHelper.getScopeUser(principal);
            customerService.updateCustomer(id, customerForm, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully.");
            return "redirect:/customers/" + id;
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
            customerService.deactivateCustomer(id, scopeUser, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Customer deactivated successfully.");
            return "redirect:/customers";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        } catch (AccessDeniedException ex) {
            return "redirect:/access-denied";
        }
    }
}
