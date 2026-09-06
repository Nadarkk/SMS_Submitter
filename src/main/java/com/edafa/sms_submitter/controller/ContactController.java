package com.edafa.sms_submitter.controller;

import com.edafa.sms_submitter.dto.AddContactReqDto;
import com.edafa.sms_submitter.dto.ContactResDto;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.DuplicateValueException;
import com.edafa.sms_submitter.exception.TargetCompanyRequiredException;
import com.edafa.sms_submitter.service.CompanyService;
import com.edafa.sms_submitter.service.ContactService;
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
@RequestMapping("/contacts")
public class ContactController {

    private final ContactService contactService;
    private final CompanyService companyService;

    @GetMapping
    public String showContactsList(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "editId", required = false) Integer editId,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model){
        log.info("Request received to show contacts list for userId: {}, targetCompanyId: {}, editId: {}",
                currentUser.getUserId(), targetCompanyId, editId);
        try{
            List<ContactResDto> contacts = contactService.getAllContacts(currentUser.getUserId(), targetCompanyId);
            model.addAttribute("contacts", contacts);

            model.addAttribute("targetCompanyId", targetCompanyId);
            // If "Edit" was clicked, load current values into DTO
            if (editId != null) {
                log.info("Loading contact details for editing, editId: {}, userId: {}", editId, currentUser.getUserId());
                ContactResDto existing = contactService.getContactById(editId, currentUser.getUserId());
                AddContactReqDto dto = new AddContactReqDto();
                dto.setName(existing.getName());
                dto.setPhoneNo(existing.getPhoneNo());
                model.addAttribute("editContactDto", dto);
                model.addAttribute("editId", editId);
            }
            return "contacts/list";
        }
        //if admin entered without specifying company
        catch (TargetCompanyRequiredException e) {
            log.warn("Target company required for viewing contacts list by userId: {}", currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            return "select-company";
        }
    }

    @GetMapping("/add")
    public String showAddForm(
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to show add contact form with targetCompanyId: {}", targetCompanyId);
        model.addAttribute("contact", new AddContactReqDto());
        model.addAttribute("targetCompanyId", targetCompanyId);
        return "contacts/add";
    }

    @PostMapping
    public String createContact(
            @Valid @ModelAttribute("contact") AddContactReqDto dto,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Processing createContact request for userId: {}, phoneNo: {}, targetCompanyId: {}",
                currentUser.getUserId(), dto.getPhoneNo(), targetCompanyId);

        if (result.hasErrors()) {
            log.warn("Validation errors while creating contact for phoneNo: {}", dto.getPhoneNo());
            model.addAttribute("targetCompanyId", targetCompanyId); // Preserve ID if validation fails
            return "contacts/add";
        }

        try {
            contactService.createContact(dto, currentUser.getUserId(), targetCompanyId);
            log.info("Contact created successfully for phoneNo: {}", dto.getPhoneNo());
        } catch (DuplicateValueException e) { // CHANGED: Catch custom DuplicateValueException
            log.warn("Duplicate contact creation attempt for phoneNo: {}", dto.getPhoneNo());
            result.rejectValue("phoneNo", "duplicate", e.getMessage());
            model.addAttribute("targetCompanyId", targetCompanyId);
            return "contacts/add";
        } catch (TargetCompanyRequiredException e) {
            log.warn("Target company required during contact creation by userId: {}", currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            return "select-company";
        }


        if (targetCompanyId != null) {
            return "redirect:/contacts?targetCompanyId=" + targetCompanyId;
        }
        return "redirect:/contacts";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to show edit form for contactId: {}, userId: {}, targetCompanyId: {}",
                id, currentUser.getUserId(), targetCompanyId);
        ContactResDto contact = contactService.getContactById(id, currentUser.getUserId());
        AddContactReqDto dto = new AddContactReqDto();
        //prefill values to easily edit
        dto.setName(contact.getName());
        dto.setPhoneNo(contact.getPhoneNo());

        model.addAttribute("contact", dto);
        model.addAttribute("contactId", id);
        model.addAttribute("targetCompanyId", targetCompanyId);
        return "contacts/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateContact(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute("editContactDto") AddContactReqDto dto,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Processing updateContact request for contactId: {}, userId: {}, targetCompanyId: {}",
                id, currentUser.getUserId(), targetCompanyId);

        if (result.hasErrors()) {
            log.warn("Validation errors during contact update for contactId: {}", id);
            // Re-render list page while maintaining edit state if validation fails
            model.addAttribute("contacts", contactService.getAllContacts(currentUser.getUserId(), targetCompanyId));
            model.addAttribute("targetCompanyId", targetCompanyId);
            model.addAttribute("editId", id);
            return "contacts/list";
        }

        try {
            contactService.updateContact(id, dto, currentUser.getUserId());
            log.info("Contact ID {} updated successfully", id);
        } catch (DuplicateValueException e) {
            log.warn("Duplicate phone number error while updating contactId: {}", id);
            result.rejectValue("phoneNo", "duplicate", "This phone number is already registered.");
            model.addAttribute("contacts", contactService.getAllContacts(currentUser.getUserId(), targetCompanyId));
            model.addAttribute("targetCompanyId", targetCompanyId);
            model.addAttribute("editId", id);
            return "contacts/list";
        }


        return targetCompanyId != null
                ? "redirect:/contacts?targetCompanyId=" + targetCompanyId
                : "redirect:/contacts";
    }

    @PostMapping("/delete/{id}")
    public String deleteContact(
            @PathVariable("id") Integer id,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            @AuthenticationPrincipal User currentUser) {
        log.info("Processing deleteContact request for contactId: {}, userId: {}, targetCompanyId: {}",
                id, currentUser.getUserId(), targetCompanyId);

        contactService.deleteContact(id, currentUser.getUserId());
        log.info("Contact ID {} deleted successfully", id);

        return targetCompanyId != null
                ? "redirect:/contacts?targetCompanyId=" + targetCompanyId
                : "redirect:/contacts";
    }
}