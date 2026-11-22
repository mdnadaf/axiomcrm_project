package com.acxiom.crm.controller.api;

import com.acxiom.crm.dto.OpportunityDto;
import com.acxiom.crm.dto.PagedResponse;
import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.OpportunityService;
import com.acxiom.crm.util.EntityMapper;
import com.acxiom.crm.viewmodel.OpportunityForm;
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
@RequestMapping("/api/opportunities")
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
@Tag(name = "Opportunity API", description = "Endpoints for managing opportunities")
public class OpportunityApiController {

    private final OpportunityService opportunityService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    @Operation(summary = "List and search opportunities with pagination")
    public ResponseEntity<PagedResponse<OpportunityDto>> getOpportunities(
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OpportunityStage stage,
            @RequestParam(required = false) OpportunityStatus status) {

        User scopeUser = scopeHelper.getScopeUser(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        Page<Opportunity> result = opportunityService.listOpportunities(scopeUser, search, stage, status, pageable);

        List<OpportunityDto> content = result.getContent().stream()
                .map(EntityMapper::toDto)
                .toList();

        PagedResponse<OpportunityDto> response = PagedResponse.<OpportunityDto>builder()
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
    @Operation(summary = "Get opportunity by ID")
    public ResponseEntity<OpportunityDto> getOpportunity(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        Opportunity opp = opportunityService.getOpportunity(id, scopeUser);
        return ResponseEntity.ok(EntityMapper.toDto(opp));
    }

    @PostMapping
    @Operation(summary = "Create a new opportunity")
    public ResponseEntity<OpportunityDto> createOpportunity(
            @Valid @RequestBody OpportunityForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        Opportunity created = opportunityService.createOpportunity(form, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(EntityMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing opportunity")
    public ResponseEntity<OpportunityDto> updateOpportunity(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        Opportunity updated = opportunityService.updateOpportunity(id, form, scopeUser, principal);
        return ResponseEntity.ok(EntityMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Delete opportunity")
    public ResponseEntity<Void> deleteOpportunity(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        opportunityService.deleteOpportunity(id, scopeUser, principal);
        return ResponseEntity.noContent().build();
    }
}
