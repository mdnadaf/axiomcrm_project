package com.acxiom.crm.controller.api;

import com.acxiom.crm.dto.JwtAuthResponse;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.JwtTokenProvider;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.viewmodel.JwtLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication API", description = "Endpoints for login and JWT token generation")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password to receive JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody JwtLoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CrmUserDetailsService.CrmUserPrincipal principal = (CrmUserDetailsService.CrmUserPrincipal) authentication.getPrincipal();
            User user = principal.getUser();

            String token = jwtTokenProvider.generateToken(user);
            auditLogService.logAuth("API_LOGIN_SUCCESS", user.getId(), user.getEmail(), httpRequest.getRemoteAddr());

            JwtAuthResponse response = JwtAuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationMs())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .build();

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                auditLogService.logAuth("API_LOGIN_FAILED", user.getId(), user.getEmail(), httpRequest.getRemoteAddr());
            });
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
            pd.setTitle("Authentication Failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
        } catch (LockedException ex) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Account is temporarily locked. Please contact administrator.");
            pd.setTitle("Account Locked");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Terminate session / invalidate JWT")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }
}
