package com.acxiom.crm.controller.api;

import com.acxiom.crm.dto.CustomerDto;
import com.acxiom.crm.dto.PagedResponse;
import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.CustomerService;
import com.acxiom.crm.util.EntityMapper;
import com.acxiom.crm.viewmodel.CustomerForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
@Tag(name = "Customer API", description = "Endpoints for managing customers")
public class CustomerApiController {

    private final CustomerService customerService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    @Operation(summary = "List and search customers with pagination")
    public ResponseEntity<PagedResponse<CustomerDto>> getCustomers(
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {

        User scopeUser = scopeHelper.getScopeUser(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        Page<Customer> result = customerService.listCustomers(scopeUser, search, active, pageable);

        List<CustomerDto> content = result.getContent().stream()
                .map(EntityMapper::toDto)
                .toList();

        PagedResponse<CustomerDto> response = PagedResponse.<CustomerDto>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerDto> getCustomer(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        Customer customer = customerService.getCustomer(id, scopeUser);
        return ResponseEntity.ok(EntityMapper.toDto(customer));
    }

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<CustomerDto> createCustomer(
            @Valid @RequestBody CustomerForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        Customer created = customerService.createCustomer(form, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(EntityMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        Customer updated = customerService.updateCustomer(id, form, scopeUser, principal);
        return ResponseEntity.ok(EntityMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Deactivate customer")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        customerService.deactivateCustomer(id, scopeUser, principal);
        return ResponseEntity.noContent().build();
    }
}
