package com.acxiom.crm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorMvcController {

    @GetMapping("/400")
    public String error400(@ModelAttribute("errorMessage") String errorMessage, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("error", "Bad Request");
        model.addAttribute("message", errorMessage != null && !errorMessage.isBlank() ? errorMessage : "The request could not be processed.");
        return "error/400";
    }

    @GetMapping("/403")
    public String error403(@ModelAttribute("errorMessage") String errorMessage, Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Forbidden");
        model.addAttribute("message", errorMessage != null && !errorMessage.isBlank() ? errorMessage : "You do not have permission to access this resource.");
        return "error/403";
    }

    @GetMapping("/404")
    public String error404(@ModelAttribute("errorMessage") String errorMessage, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Not Found");
        model.addAttribute("message", errorMessage != null && !errorMessage.isBlank() ? errorMessage : "The requested resource was not found.");
        return "error/404";
    }

    @GetMapping("/500")
    public String error500(@ModelAttribute("errorMessage") String errorMessage, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Internal Server Error");
        model.addAttribute("message", errorMessage != null && !errorMessage.isBlank() ? errorMessage : "An unexpected error occurred. Please contact the administrator.");
        return "error/500";
    }
}
