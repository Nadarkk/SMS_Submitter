package com.edafa.sms_submitter.controller;

import com.edafa.sms_submitter.dto.DashboardStatsDto;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.repository.UserRepo;
import com.edafa.sms_submitter.service.CompanyService;
import com.edafa.sms_submitter.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;
    private final CompanyService companyService;
    private final UserRepo userRepo;

    @GetMapping({"/", "/dashboard", "/index"})
    public String index(
            Authentication authentication,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to dashboard");
            return "redirect:/login";
        }

        // Safely fetch fresh user entity from database using the authenticated username/phone
        String username = authentication.getName();
        User currentUser = userRepo.findByPhoneNo(username)
                .orElseGet(() -> userRepo.findAll().stream()
                        .filter(u -> u.getPhoneNo().equals(username))
                        .findFirst()
                        .orElse(null));

        if (currentUser == null) {
            log.error("Authenticated user not found in database for username: {}", username);
            return "redirect:/login";
        }

        try {
            boolean isAdmin = currentUser.getRole() != null &&
                    currentUser.getRole().name().equalsIgnoreCase("ADMIN");

            DashboardStatsDto stats = dashboardService.getDashboardStats(currentUser.getUserId(), targetCompanyId);
            String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();

            model.addAttribute("stats", stats);
            model.addAttribute("fullName", fullName);
            model.addAttribute("targetCompanyId", targetCompanyId);
            model.addAttribute("isAdmin", isAdmin);

            if (isAdmin) {
                try{
                    model.addAttribute("companies", companyService.getAllCompanies());
                } catch (Exception e) {
                    log.error("Error fetching companies for admin dashboard: ", e);
                    model.addAttribute("errorMessage", "Error loading companies: " + e.getMessage());
                }
            }

            return "index";
        } catch (Exception e) {
            log.error("CRITICAL ERROR inside dashboard rendering: ", e);
            model.addAttribute("errorMessage", "Error loading dashboard: " + e.getMessage());
            return "index";
        }
    }
}