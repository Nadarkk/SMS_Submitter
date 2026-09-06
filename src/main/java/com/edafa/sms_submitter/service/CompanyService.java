package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.dto.CompanyReqDto;
import com.edafa.sms_submitter.dto.CompanyResDto;
import com.edafa.sms_submitter.entity.Company;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.mapper.CompanyMapper;
import com.edafa.sms_submitter.repository.CompanyRepo;
import com.edafa.sms_submitter.repository.UserRepo;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepo companyRepo;
    private final UserRepo userRepo;
    private final CompanyMapper companyMapper;
    private final AccessService accessService;
    // Create (ADMIN only)[cite: 10]
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CompanyResDto createCompany(CompanyReqDto dto) {
        log.info("Creating new company with name: {}", dto.getCompanyName());
        if (companyRepo.existsByCompanyName(dto.getCompanyName())) {
            log.warn("Company creation failed: company name '{}' already exists", dto.getCompanyName());
            throw new DuplicateValueException("Company with name '" + dto.getCompanyName() + "' already exists");
        }

        Company company = companyMapper.toEntity(dto);
        Company savedCompany = companyRepo.save(company);
        log.info("Company created successfully with ID: {}", savedCompany.getCompanyId());
        return companyMapper.toDto(savedCompany);
    }

    // Read (ADMIN or MANAGER of that company)[cite: 10]
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional(readOnly = true)
    public CompanyResDto getCompanyById(Integer companyId, Integer requesterId) {
        log.info("Fetching company by ID: {} for requesterId: {}", companyId, requesterId);
        User requester = userRepo.findById(requesterId)
                .orElseThrow(() -> {
                    log.error("Requester with ID {} not found", requesterId);
                    return new ResourceNotFoundException("Requester with ID " + requesterId + " not found");
                });

        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company with ID {} not found", companyId);
                    return new ResourceNotFoundException("Company with ID " + companyId + " not found");
                });
        accessService.checkAccess(requester, companyId);
        return companyMapper.toDto(company);
    }

    // Read All Companies (ADMIN only)[cite: 10]
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<CompanyResDto> getAllCompanies() {
        log.info("Fetching all companies");
        List<CompanyResDto> companies = companyRepo.findAll()
                .stream()
                .map(companyMapper::toDto)
                .collect(Collectors.toList());
        log.info("Retrieved {} companies", companies.size());
        return companies;
    }

    // Update Company (ADMIN only)[cite: 10]
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyResDto updateCompany(Integer companyId, CompanyReqDto dto) {
        log.info("Updating company with ID: {}", companyId);
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company update failed: company ID {} not found", companyId);
                    return new ResourceNotFoundException("Company with ID " + companyId + " not found");
                });
        if (dto.getCompanyName() != null && !dto.getCompanyName().trim().isBlank()) {
            if (!company.getCompanyName().equals(dto.getCompanyName()) && companyRepo.existsByCompanyName(dto.getCompanyName())) {
                log.warn("Company update failed: name '{}' already exists", dto.getCompanyName());
                throw new DuplicateValueException("Company with name '" + dto.getCompanyName() + "' already exists");
            }
            company.setCompanyName(dto.getCompanyName());
        }
        Company updatedCompany = companyRepo.save(company);
        log.info("Company with ID: {} updated successfully", companyId);
        return companyMapper.toDto(updatedCompany);
    }

    // Delete Company (ADMIN only)[cite: 10]
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteCompany(Integer companyId) {
        log.info("Deleting company with ID: {}", companyId);
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company deletion failed: company ID {} not found", companyId);
                    return new ResourceNotFoundException("Company with ID " + companyId + " not found");
                });

        companyRepo.delete(company);
        log.info("Company with ID: {} deleted successfully", companyId);
    }

}