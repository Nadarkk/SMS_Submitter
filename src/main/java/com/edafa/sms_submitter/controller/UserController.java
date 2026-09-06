package com.edafa.sms_submitter.controller;

import com.edafa.sms_submitter.dto.UserReqDto;
import com.edafa.sms_submitter.dto.UserResDto;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.service.CompanyService;
import com.edafa.sms_submitter.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CompanyService companyService;

    // View User List (ADMIN only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to list users for targetCompanyId: {}, requested by ADMIN userId: {}",
                targetCompanyId, currentUser.getUserId());

        List<UserResDto> users = userService.getAllUsers(targetCompanyId);
        model.addAttribute("users", users);
        model.addAttribute("companies", companyService.getAllCompanies());
        model.addAttribute("targetCompanyId", targetCompanyId);
        return "users/list";
    }

    // Show Create User Form
    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to show create user form for targetCompanyId: {}", targetCompanyId);

        UserReqDto dto = new UserReqDto();
        if (targetCompanyId != null) {
            dto.setCompanyId(targetCompanyId);
        }

        model.addAttribute("userReqDto", dto);
        model.addAttribute("roles", Role.values());
        model.addAttribute("companies", companyService.getAllCompanies());
        return "users/add";
    }

    // Handle Create User Submission (ADMIN only)
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(
            @Valid @ModelAttribute("userReqDto") UserReqDto userReqDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        log.info("Processing createUser request for phoneNo: {}, companyId: {}",
                userReqDto.getPhoneNo(), userReqDto.getCompanyId());

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors while creating user with phoneNo: {}", userReqDto.getPhoneNo());
            model.addAttribute("roles", Role.values());
            model.addAttribute("companies", companyService.getAllCompanies());
            return "users/add";
        }
        try {
            userService.createUser(userReqDto);
            log.info("User created successfully with phoneNo: {}", userReqDto.getPhoneNo());
        } catch (DuplicateValueException e) {
            log.warn("Duplicate value error during user creation for phoneNo: {}", userReqDto.getPhoneNo());
            bindingResult.rejectValue("phoneNo", "duplicate", e.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("companies", companyService.getAllCompanies());
            return "users/add";
        }

        redirectAttributes.addFlashAttribute("successMessage", "User created successfully!");
        return "redirect:/users?targetCompanyId=" + userReqDto.getCompanyId();
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleUserStatus(
            @PathVariable("id") Integer userId,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            RedirectAttributes redirectAttributes) {
        log.info("Processing toggleUserStatus request for targetUserId: {}, targetCompanyId: {}", userId, targetCompanyId);

        userService.toggleUserStatus(userId);
        log.info("User status toggled successfully for userId: {}", userId);
        redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully!");

        return "redirect:/users" + (targetCompanyId != null ? "?targetCompanyId=" + targetCompanyId : "");
    }
}