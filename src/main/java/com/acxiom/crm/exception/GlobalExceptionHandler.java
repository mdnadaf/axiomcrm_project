package com.acxiom.crm.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // Helper: decide if the client wants JSON or an MVC page
    // -----------------------------------------------------------------------
    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private boolean isMvcRequest(HttpServletRequest request) {
        return !isApiRequest(request);
    }

    // -----------------------------------------------------------------------
    // BusinessException → 400
    // -----------------------------------------------------------------------
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        if (isMvcRequest(request)) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/error/400";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Business Rule Violation");
        pd.setType(URI.create("urn:acxiom:error:business"));
        return pd;
    }

    // -----------------------------------------------------------------------
    // ResourceNotFoundException → 404
    // -----------------------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        if (isMvcRequest(request)) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/error/404";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setType(URI.create("urn:acxiom:error:not-found"));
        return pd;
    }

    // -----------------------------------------------------------------------
    // DuplicateResourceException → 409
    // -----------------------------------------------------------------------
    @ExceptionHandler(DuplicateResourceException.class)
    public Object handleDuplicateResource(DuplicateResourceException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        if (isMvcRequest(request)) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/error/400";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Duplicate Resource");
        pd.setType(URI.create("urn:acxiom:error:conflict"));
        return pd;
    }

    // -----------------------------------------------------------------------
    // AccessDeniedException → 403
    // -----------------------------------------------------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        if (isMvcRequest(request)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You do not have permission to access this resource.");
            return "redirect:/error/403";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.");
        pd.setTitle("Access Denied");
        pd.setType(URI.create("urn:acxiom:error:forbidden"));
        return pd;
    }

    // -----------------------------------------------------------------------
    // MethodArgumentNotValidException → 400 with field errors
    // -----------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                               HttpServletRequest request,
                                               RedirectAttributes redirectAttributes) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        String summary = fieldErrors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));

        if (isMvcRequest(request)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + summary);
            return "redirect:/error/400";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Validation failed. See 'fieldErrors' for details.");
        pd.setTitle("Validation Error");
        pd.setType(URI.create("urn:acxiom:error:validation"));
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    // -----------------------------------------------------------------------
    // ConstraintViolationException → 400
    // -----------------------------------------------------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public Object handleConstraintViolation(ConstraintViolationException ex,
                                            HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    // strip method name prefix to avoid leaking internals
                    int dot = path.lastIndexOf('.');
                    return (dot >= 0 ? path.substring(dot + 1) : path) + ": " + cv.getMessage();
                })
                .collect(Collectors.joining("; "));

        if (isMvcRequest(request)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + detail);
            return "redirect:/error/400";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation: " + detail);
        pd.setTitle("Constraint Violation");
        pd.setType(URI.create("urn:acxiom:error:constraint"));
        return pd;
    }

    // -----------------------------------------------------------------------
    // Catch-all → 500 (never expose internals)
    // -----------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        // Log internally (use SLF4J if logger is available)
        // Do NOT surface the original message – it may contain SQL or stack info
        if (isMvcRequest(request)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "An unexpected error occurred. Please try again later.");
            return "redirect:/error/500";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("urn:acxiom:error:internal"));
        return pd;
    }
}
