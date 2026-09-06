package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.dto.AddContactReqDto;
import com.edafa.sms_submitter.dto.ContactResDto;
import com.edafa.sms_submitter.entity.*;
import com.edafa.sms_submitter.mapper.ContactMapper;
import com.edafa.sms_submitter.repository.CompanyRepo;
import com.edafa.sms_submitter.repository.ContactRepo;
import com.edafa.sms_submitter.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMapper contactMapper;
    private final ContactRepo contactRepo;
    private final CompanyRepo companyRepo;
    private final UserRepo userRepo;
    private final AccessService accessService;

    //Create: ADMIN (any company using targetCompanyId) or MANAGER (own company only)[cite: 11]
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public ContactResDto createContact(AddContactReqDto dto, Integer userId, Integer targetCompanyId) {
        log.info("Creating contact for userId: {}, phoneNo: {}, targetCompanyId: {}", userId, dto.getPhoneNo(), targetCompanyId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during contact creation", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Integer companyId = accessService.resolveCompanyId(user, targetCompanyId);
        if (contactRepo.existsContactByPhoneNoAndCompanyCompanyId(dto.getPhoneNo(), companyId)) {
            log.warn("Contact creation failed: phone number {} already exists in companyId: {}", dto.getPhoneNo(), companyId);
            throw new DuplicateValueException("Contact with phone number " + dto.getPhoneNo() + " already exists");
        }
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company with ID {} not found during contact creation", companyId);
                    return new ResourceNotFoundException("Company with ID " + companyId + " not found");
                });
        Contact contact = contactMapper.toEntity(dto, company);
        Contact savedContact = contactRepo.save(contact);
        log.info("Contact created successfully with ID: {}", savedContact.getContactId());
        return contactMapper.toDto(savedContact);
    }

    //Read: any role, own company only (ADMIN sees all)[cite: 11]

    @Transactional(readOnly = true)
    public ContactResDto getContactById(Integer contactId, Integer userId) {
        log.info("Fetching contact by ID: {} for userId: {}", contactId, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during contact fetch", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> {
                    log.error("Contact with ID {} not found", contactId);
                    return new ResourceNotFoundException("Contact with ID " + contactId + " not found");
                });

        accessService.checkAccess(user, contact.getCompany().getCompanyId());
        return contactMapper.toDto(contact);
    }

    //Update: ADMIN (any) or MANAGER (own company only)[cite: 11]
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public ContactResDto updateContact(Integer contactId, AddContactReqDto dto, Integer userId) {
        log.info("Updating contact ID: {} by userId: {}", contactId, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during contact update", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> {
                    log.error("Contact with ID {} not found during update", contactId);
                    return new ResourceNotFoundException("Contact with ID " + contactId + " not found");
                });
        Integer companyId = contact.getCompany().getCompanyId();
        accessService.checkAccess(user, companyId);

        if (dto.getPhoneNo() != null && contactRepo.existsContactByPhoneNoAndCompanyCompanyIdAndContactIdNot(dto.getPhoneNo(), companyId, contactId )) {
            log.warn("Contact update failed: phone number {} already exists in companyId: {}", dto.getPhoneNo(), companyId);
            throw new DuplicateValueException("Contact with phone number " + dto.getPhoneNo() + " already exists");
        }
        ContactResDto updatedDto = contactMapper.updateEntityFromDto(dto, contact);
        log.info("Contact ID: {} updated successfully", contactId);
        return updatedDto;
    }

    //Delete: ADMIN (any) or MANAGER (own company only)[cite: 11]
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Transactional
    public void deleteContact(Integer contactId, Integer userId) {
        log.info("Deleting contact ID: {} by userId: {}", contactId, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during contact deletion", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });

        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> {
                    log.error("Contact with ID {} not found during deletion", contactId);
                    return new ResourceNotFoundException("Contact with ID " + contactId + " not found");
                });

        accessService.checkAccess(user, contact.getCompany().getCompanyId());
        contactRepo.delete(contact);
        contactRepo.flush();
        log.info("Contact ID: {} deleted successfully", contactId);
    }

    //Returning all contacts for a company — ADMIN (any via targetCompanyId) or MANAGER/USER (own company)[cite: 11]
    @Transactional(readOnly = true)
    public List<ContactResDto> getAllContacts(Integer userId, Integer targetCompanyId) {
        log.info("Fetching all contacts for userId: {}, targetCompanyId: {}", userId, targetCompanyId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during contacts list lookup", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Integer companyId = accessService.resolveCompanyId(user, targetCompanyId);
        List<Contact> contacts = contactRepo.findAllByCompanyCompanyId(companyId);
            log.info("Retrieved {} contacts for companyId: {}", contacts.size(), companyId);
        return contacts.stream()
                .map(contactMapper::toDto)
                .toList();
    }
}