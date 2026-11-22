package com.acxiom.crm.controller.api;

import com.acxiom.crm.dto.LeadDto;
import com.acxiom.crm.dto.PagedResponse;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.LeadService;
import com.acxiom.crm.util.EntityMapper;
import com.acxiom.crm.viewmodel.LeadForm;
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
@RequestMapping("/api/leads")
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
@Tag(name = "Lead API", description = "Endpoints for managing leads")
public class LeadApiController {

    private final LeadService leadService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    @Operation(summary = "List and search leads with pagination")
    public ResponseEntity<PagedResponse<LeadDto>> getLeads(
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        User scopeUser = scopeHelper.getScopeUser(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        Page<Lead> result = leadService.listLeads(scopeUser, search, status, pageable);

        List<LeadDto> content = result.getContent().stream()
                .map(EntityMapper::toDto)
                .toList();

        PagedResponse<LeadDto> response = PagedResponse.<LeadDto>builder()
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
    @Operation(summary = "Get lead by ID")
    public ResponseEntity<LeadDto> getLead(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        Lead lead = leadService.getLead(id, scopeUser);
        return ResponseEntity.ok(EntityMapper.toDto(lead));
    }

    @PostMapping
    @Operation(summary = "Create a new lead")
    public ResponseEntity<LeadDto> createLead(
            @Valid @RequestBody LeadForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        Lead created = leadService.createLead(form, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(EntityMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing lead")
    public ResponseEntity<LeadDto> updateLead(
            @PathVariable Long id,
            @Valid @RequestBody LeadForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        Lead updated = leadService.updateLead(id, form, scopeUser, principal);
        return ResponseEntity.ok(EntityMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Delete lead")
    public ResponseEntity<Void> deleteLead(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        leadService.deleteLead(id, scopeUser, principal);
        return ResponseEntity.noContent().build();
    }
}
