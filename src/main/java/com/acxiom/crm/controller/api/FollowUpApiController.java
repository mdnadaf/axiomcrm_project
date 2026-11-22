package com.acxiom.crm.controller.api;

import com.acxiom.crm.dto.FollowUpDto;
import com.acxiom.crm.dto.PagedResponse;
import com.acxiom.crm.entity.FollowUp;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.FollowUpService;
import com.acxiom.crm.util.EntityMapper;
import com.acxiom.crm.viewmodel.FollowUpForm;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/followups")
@SecurityRequirement(name = "BearerAuth")
@RequiredArgsConstructor
@Tag(name = "Follow-Up API", description = "Endpoints for managing follow-ups")
public class FollowUpApiController {

    private final FollowUpService followUpService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    @Operation(summary = "List and search follow-ups with pagination")
    public ResponseEntity<PagedResponse<FollowUpDto>> getFollowUps(
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "followUpDate") String sort,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) FollowUpStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        User scopeUser = scopeHelper.getScopeUser(principal);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sort));
        Page<FollowUp> result = followUpService.listFollowUps(scopeUser, status, fromDate, toDate, pageable);

        List<FollowUpDto> content = result.getContent().stream()
                .map(EntityMapper::toDto)
                .toList();

        PagedResponse<FollowUpDto> response = PagedResponse.<FollowUpDto>builder()
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
    @Operation(summary = "Get follow-up by ID")
    public ResponseEntity<FollowUpDto> getFollowUp(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        FollowUp followUp = followUpService.getFollowUp(id, scopeUser);
        return ResponseEntity.ok(EntityMapper.toDto(followUp));
    }

    @PostMapping
    @Operation(summary = "Create a new follow-up")
    public ResponseEntity<FollowUpDto> createFollowUp(
            @Valid @RequestBody FollowUpForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        FollowUp created = followUpService.createFollowUp(form, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(EntityMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing follow-up")
    public ResponseEntity<FollowUpDto> updateFollowUp(
            @PathVariable Long id,
            @Valid @RequestBody FollowUpForm form,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        FollowUp updated = followUpService.updateFollowUp(id, form, scopeUser, principal);
        return ResponseEntity.ok(EntityMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Delete follow-up")
    public ResponseEntity<Void> deleteFollowUp(
            @PathVariable Long id,
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        followUpService.deleteFollowUp(id, scopeUser, principal);
        return ResponseEntity.noContent().build();
    }
}
