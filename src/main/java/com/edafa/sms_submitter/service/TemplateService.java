package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.dto.TemplateReqDto;
import com.edafa.sms_submitter.dto.TemplateResDto;
import com.edafa.sms_submitter.entity.Company;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.entity.Template;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.mapper.TemplateMapper;
import com.edafa.sms_submitter.repository.CompanyRepo;
import com.edafa.sms_submitter.repository.TemplateRepo;
import com.edafa.sms_submitter.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateRepo templateRepo;
    private final TemplateMapper templateMapper;
    private final UserRepo userRepo;
    private final CompanyRepo companyRepo;
    private final AccessService accessControlService;

    //Create — ADMIN  or MANAGER (own company only)[cite: 14]
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public TemplateResDto createTemplate(TemplateReqDto dto, Integer userId, Integer targetCompanyId) {
        log.info("Creating template name: {} for userId: {}, targetCompanyId: {}", dto.getName(), userId, targetCompanyId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during template creation", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });

        Integer companyId = accessControlService.resolveCompanyId(user, targetCompanyId);
        if (templateRepo.existsTemplateByNameAndCompanyCompanyId(dto.getName(), companyId)) {
            log.warn("Template creation failed: template name '{}' already exists in companyId: {}", dto.getName(), companyId);
            throw new DuplicateValueException("Template with name " + dto.getName() + " already exists");
        }
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company with ID {} not found during template creation", companyId);
                    return new ResourceNotFoundException("Company with ID " + companyId + " not found");
                });
        Template template = templateMapper.toEntity(dto, company);
        Template savedTemplate = templateRepo.save(template);
        log.info("Template created successfully with ID: {}", savedTemplate.getTemplateId());
        return templateMapper.toDto(savedTemplate);
    }

    //Read — all allowed (own company)[cite: 14]
    @Transactional(readOnly = true)
    public TemplateResDto getTemplateById(Integer templateId, Integer userId) {
        log.info("Fetching template by ID: {} for userId: {}", templateId, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during template fetch", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Template template = templateRepo.findById(templateId)
                .orElseThrow(() -> {
                    log.error("Template with ID {} not found", templateId);
                    return new ResourceNotFoundException("Template with ID " + templateId + " not found");
                });

        accessControlService.checkAccess(user, template.getCompany().getCompanyId());
        return templateMapper.toDto(template);
    }

    //Update — ADMIN or MANAGER (own company only)[cite: 14]
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public TemplateResDto updateTemplate(Integer templateId, TemplateReqDto dto, Integer userId) {
        log.info("Updating template ID: {} by userId: {}", templateId, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during template update", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });

        Template template = templateRepo.findById(templateId)
                .orElseThrow(() -> {
                    log.error("Template with ID {} not found during update", templateId);
                    return new ResourceNotFoundException("Template with ID " + templateId + " not found");
                });

        accessControlService.checkAccess(user, template.getCompany().getCompanyId());

        template.setName(dto.getName());
        template.setBody(dto.getBody());
        Template updatedTemplate = templateRepo.save(template);
        log.info("Template ID: {} updated successfully", templateId);
        return templateMapper.toDto(updatedTemplate);
    }

    //Delete — ADMIN or MANAGER (own company only)[cite: 14]
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public void deleteTemplate(Integer templateId, Integer userId) {
        log.info("Deleting template ID: {} by userId: {}", templateId, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during template deletion", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Template template = templateRepo.findById(templateId)
                .orElseThrow(() -> {
                    log.error("Template with ID {} not found during deletion", templateId);
                    return new ResourceNotFoundException("Template with ID " + templateId + " not found");
                });

        accessControlService.checkAccess(user, template.getCompany().getCompanyId());
        templateRepo.delete(template);
        log.info("Template ID: {} deleted successfully", templateId);
    }

    //List — ALL(own company)[cite: 14]
    @Transactional(readOnly = true)
    public List<TemplateResDto> getAllTemplatesByCompany(Integer userId, Integer targetCompanyId) {
        log.info("Fetching templates for userId: {}, targetCompanyId: {}", userId, targetCompanyId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during templates list lookup", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Integer companyId = accessControlService.resolveCompanyId(user, targetCompanyId);
        List<Template> templates = templateRepo.findByCompanyCompanyId(companyId);
        log.info("Retrieved {} templates for companyId: {}", templates.size(), companyId);
        return templates.stream()
                .map(templateMapper::toDto)
                .toList();
    }
}