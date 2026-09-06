package com.edafa.sms_submitter.controller;

import com.edafa.sms_submitter.dto.TemplateReqDto;
import com.edafa.sms_submitter.dto.TemplateResDto;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.exception.TargetCompanyRequiredException;
import com.edafa.sms_submitter.service.CompanyService;
import com.edafa.sms_submitter.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/template")
public class TemplateController {

    private final TemplateService templateService;
    private final CompanyService companyService;

    @GetMapping
    public String showTemplatesList(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "editId", required = false) Integer editId,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to show template list for userId: {}, targetCompanyId: {}, editId: {}",
                currentUser.getUserId(), targetCompanyId, editId);
        try {
            List<TemplateResDto> templates = templateService.getAllTemplatesByCompany(currentUser.getUserId(), targetCompanyId);
            model.addAttribute("templates", templates);
            model.addAttribute("targetCompanyId", targetCompanyId);

            if (editId != null) {
                log.info("Loading template details for editId: {}, userId: {}", editId, currentUser.getUserId());
                TemplateResDto existing = templateService.getTemplateById(editId, currentUser.getUserId());
                TemplateReqDto dto = new TemplateReqDto();
                dto.setName(existing.getName());
                dto.setBody(existing.getBody());
                model.addAttribute("editTemplateDto", dto);
                model.addAttribute("editId", editId);
            }
            return "template/list";
        } catch (TargetCompanyRequiredException e) {
            log.warn("Target company required for viewing template list by userId: {}", currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            model.addAttribute("redirectUrl", "/template");
            return "select-company";
        }
    }

    @GetMapping("/add")
    public String showAddForm(
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to show add template form with targetCompanyId: {}", targetCompanyId);
        model.addAttribute("template", new TemplateReqDto());
        model.addAttribute("targetCompanyId", targetCompanyId);
        return "template/add";
    }

    @PostMapping
    public String createTemplate(
            @Valid @ModelAttribute("template") TemplateReqDto dto,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Processing createTemplate request for name: {}, userId: {}, targetCompanyId: {}",
                dto.getName(), currentUser.getUserId(), targetCompanyId);

        if (result.hasErrors()) {
            log.warn("Validation errors while creating template name: {}", dto.getName());
            model.addAttribute("targetCompanyId", targetCompanyId);
            return "template/add";
        }

        try {
            templateService.createTemplate(dto, currentUser.getUserId(), targetCompanyId);
            log.info("Template created successfully: {}", dto.getName());
        } catch (DuplicateValueException e) {
            log.warn("Duplicate template creation attempt for name: {}", dto.getName());
            result.reject("duplicate.template", "A template with this name already exists.");
            model.addAttribute("targetCompanyId", targetCompanyId);
            return "template/add";
        } catch (TargetCompanyRequiredException e) {
            log.warn("Target company required during template creation by userId: {}", currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            model.addAttribute("redirectUrl", "/template");
            return "select-company";
        }

        return targetCompanyId != null
                ? "redirect:/template?targetCompanyId=" + targetCompanyId
                : "redirect:/template";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to show edit form for templateId: {}, userId: {}, targetCompanyId: {}",
                id, currentUser.getUserId(), targetCompanyId);
        TemplateResDto template = templateService.getTemplateById(id, currentUser.getUserId());
        TemplateReqDto dto = new TemplateReqDto();
        dto.setName(template.getName());
        dto.setBody(template.getBody());

        model.addAttribute("template", dto);
        model.addAttribute("templateId", id);
        model.addAttribute("targetCompanyId", targetCompanyId);
        return "template/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateTemplate(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("editTemplateDto") TemplateReqDto dto,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Processing updateTemplate request for templateId: {}, userId: {}, targetCompanyId: {}",
                id, currentUser.getUserId(), targetCompanyId);

        if (result.hasErrors()) {
            log.warn("Validation errors during template update for templateId: {}", id);
            try {
                model.addAttribute("template", templateService.getAllTemplatesByCompany(currentUser.getUserId(), targetCompanyId));
            } catch (TargetCompanyRequiredException e) {
                log.warn("Target company required during template update error handling for userId: {}", currentUser.getUserId());
                model.addAttribute("companies", companyService.getAllCompanies());
                model.addAttribute("redirectUrl", "/template");
                return "select-company";
            }
            model.addAttribute("targetCompanyId", targetCompanyId);
            model.addAttribute("editId", id);
            return "template/list";
        }

        try {
            templateService.updateTemplate(id, dto, currentUser.getUserId());
            log.info("Template ID {} updated successfully", id);
        } catch (DuplicateValueException e) {
            log.warn("Duplicate template name error while updating templateId: {}", id);
            result.rejectValue("name", "duplicate", "A template with this name already exists.");

            try {
                model.addAttribute("templates", templateService.getAllTemplatesByCompany(currentUser.getUserId(), targetCompanyId));
            } catch (TargetCompanyRequiredException ex) {
                log.warn("Target company required during template update duplicate catch for userId: {}", currentUser.getUserId());
                model.addAttribute("companies", companyService.getAllCompanies());
                model.addAttribute("redirectUrl", "/template");
                return "select-company";
            }

            model.addAttribute("targetCompanyId", targetCompanyId);
            model.addAttribute("editId", id);
            return "template/list";
        } catch (TargetCompanyRequiredException e) {
            log.warn("Target company required during template update for userId: {}", currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            model.addAttribute("redirectUrl", "/template");
            return "select-company";
        }

        return targetCompanyId != null
                ? "redirect:/template?targetCompanyId=" + targetCompanyId
                : "redirect:/template";
    }

    @PostMapping("/delete/{id}")
    public String deleteTemplate(
            @PathVariable("id") Integer id,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            @AuthenticationPrincipal User currentUser) {
        log.info("Processing deleteTemplate request for templateId: {}, userId: {}, targetCompanyId: {}",
                id, currentUser.getUserId(), targetCompanyId);

        templateService.deleteTemplate(id, currentUser.getUserId());
        log.info("Template ID {} deleted successfully", id);

        return targetCompanyId != null
                ? "redirect:/template?targetCompanyId=" + targetCompanyId
                : "redirect:/template";
    }
}