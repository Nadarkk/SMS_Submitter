package com.edafa.sms_submitter.controller;

import com.edafa.sms_submitter.dto.CompanyReqDto;
import com.edafa.sms_submitter.dto.CompanyResDto;
import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public String listCompanies(Model model) {
        log.info("Request received to list all companies");
        List<CompanyResDto> companies = companyService.getAllCompanies();
        model.addAttribute("companies", companies);
        return "companies/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        log.info("Request received to show add company form");
        model.addAttribute("companyReqDto", new CompanyReqDto());
        return "companies/add";
    }

    @PostMapping("/add")
    public String addCompany(
            @Valid @ModelAttribute("companyReqDto") CompanyReqDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        log.info("Processing addCompany request for company name: {}", dto.getCompanyName());

        if (result.hasErrors()) {
            log.warn("Validation errors while adding company: {}", dto.getCompanyName());
            return "companies/add";
        }

        try {
            companyService.createCompany(dto);
            log.info("Company created successfully: {}", dto.getCompanyName());
            redirectAttributes.addFlashAttribute("successMessage", "Company created successfully!");
            return "redirect:/companies";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument while adding company: {}", e.getMessage());
            result.rejectValue("companyName", "error.companyReqDto", e.getMessage());
            return "companies/add";
        } catch (DuplicateValueException e) {
            log.warn("Duplicate company name error for: {}", dto.getCompanyName());
            result.rejectValue("companyName", "duplicate", e.getMessage());
            return "companies/add";
        }
    }
}