package com.acxiom.crm.controller;

import com.acxiom.crm.enums.Role;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.exception.DuplicateResourceException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.UserService;
import com.acxiom.crm.viewmodel.UserForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    public String list(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(defaultValue = "fullName") String sort,
                       @RequestParam(defaultValue = "ASC") String direction,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) Boolean active,
                       Model model) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        var users = userService.listUsers(search, role, active, pageable);
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedActive", active);
        model.addAttribute("roles", Role.values());
        model.addAttribute("isAdmin", scopeHelper.isAdmin(principal));
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        return "user/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        UserForm form = new UserForm();
        form.setRole("ROLE_SALES_EXECUTIVE");
        form.setActive(true);
        model.addAttribute("userForm", form);
        model.addAttribute("roles", Role.values());
        return "user/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("userForm") UserForm userForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "user/form";
        }
        try {
            var user = userService.createUser(userForm, principal);
            redirectAttributes.addFlashAttribute("successMessage", "User '" + user.getEmail() + "' created successfully.");
            return "redirect:/admin/users/" + user.getId();
        } catch (DuplicateResourceException | BusinessException ex) {
            bindingResult.reject("error.userForm", ex.getMessage());
            model.addAttribute("roles", Role.values());
            return "user/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         Model model) {
        try {
            var user = userService.getUser(id);
            model.addAttribute("user", user);
            model.addAttribute("isAdmin", scopeHelper.isAdmin(principal));
            return "user/detail";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            var user = userService.getUser(id);
            model.addAttribute("userForm", userService.toForm(user));
            model.addAttribute("userId", id);
            model.addAttribute("roles", Role.values());
            return "user/form";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        }
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         @Valid @ModelAttribute("userForm") UserForm userForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", Role.values());
            return "user/form";
        }
        try {
            userService.updateUser(id, userForm, principal);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
            return "redirect:/admin/users/" + id;
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("email", "error.userForm", ex.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("roles", Role.values());
            return "user/form";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        }
    }

    @PostMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleActive(@PathVariable Long id,
                               @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActive(id, principal);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
            return "redirect:/admin/users/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        }
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public String unlock(@PathVariable Long id,
                         @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.unlockAccount(id, principal);
            redirectAttributes.addFlashAttribute("successMessage", "User account unlocked successfully.");
            return "redirect:/admin/users/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id, newPassword, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully.");
            return "redirect:/admin/users/" + id;
        } catch (ResourceNotFoundException ex) {
            return "redirect:/error/404";
        }
    }
}
