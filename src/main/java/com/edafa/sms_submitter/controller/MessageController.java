package com.edafa.sms_submitter.controller;

import com.edafa.sms_submitter.dto.ContactResDto;
import com.edafa.sms_submitter.dto.MessageReqDto;
import com.edafa.sms_submitter.dto.MessageResDto;
import com.edafa.sms_submitter.dto.TemplateResDto;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.TargetCompanyRequiredException;
import com.edafa.sms_submitter.service.CompanyService;
import com.edafa.sms_submitter.service.ContactService;
import com.edafa.sms_submitter.service.MessageService;
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
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;
    private final ContactService contactService;
    private final TemplateService templateService;
    private final CompanyService companyService;

    @GetMapping
    public String listMessages(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to list messages for userId: {}, targetCompanyId: {}",
                currentUser.getUserId(), targetCompanyId);
        try {
            List<MessageResDto> messages = messageService.getAllMessagesByCompany(currentUser.getUserId(), targetCompanyId);
            model.addAttribute("messages", messages);
            model.addAttribute("targetCompanyId", targetCompanyId);
            return "messages/list";
        } catch (TargetCompanyRequiredException e) {
            log.warn("Target company required for viewing message list by userId: {}", currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            model.addAttribute("redirectUrl", "/messages");
            return "select-company";
        }
    }

    @GetMapping("/send")
    public String showSendForm(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            @RequestParam(name = "selectedTemplateId", required = false) Integer selectedTemplateId,
            Model model) {
        log.info("Request received to show send message form for userId: {}, targetCompanyId: {}, selectedTemplateId: {}",
                currentUser.getUserId(), targetCompanyId, selectedTemplateId);
        try {
            populateSendForm(model, currentUser, targetCompanyId, selectedTemplateId, null);
            return "messages/send";
        } catch (TargetCompanyRequiredException e) {
            log.warn("Target company required for send message form by userId: {}", currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            model.addAttribute("redirectUrl", "/messages/send");
            return "select-company";
        }
    }

    @PostMapping("/send")
    public String sendMessage(
            @Valid @ModelAttribute("messageReqDto") MessageReqDto dto,
            BindingResult result,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Processing sendMessage request for userId: {}, targetCompanyId: {}",
                currentUser.getUserId(), targetCompanyId);

        if (result.hasErrors()) {
            log.warn("Validation errors in send message request by userId: {}", currentUser.getUserId());
            try {
                populateSendForm(model, currentUser, targetCompanyId, null, dto);
            } catch (TargetCompanyRequiredException e) {
                log.warn("Target company required during validation error handling for userId: {}", currentUser.getUserId());
                model.addAttribute("companies", companyService.getAllCompanies());
                model.addAttribute("redirectUrl", "/messages/send");
                return "select-company";
            }
            return "messages/send";
        }

        messageService.sendMessage(dto, currentUser.getUserId(), targetCompanyId);
        log.info("Message sent successfully by userId: {}", currentUser.getUserId());

        return targetCompanyId != null
                ? "redirect:/messages?targetCompanyId=" + targetCompanyId
                : "redirect:/messages";
    }

    // Supports both /messages/1 and /messages/details/1 endpoints
    @GetMapping({"/details/{id}", "/{id}"})
    public String viewMessageDetails(
            @PathVariable("id") Integer messageId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(name = "targetCompanyId", required = false) Integer targetCompanyId,
            Model model) {
        log.info("Request received to view message details for messageId: {}, userId: {}, targetCompanyId: {}",
                messageId, currentUser.getUserId(), targetCompanyId);
        try {
            MessageResDto message = messageService.getMessageById(messageId, currentUser.getUserId());
            if (message == null) {
                return targetCompanyId != null
                        ? "redirect:/messages?targetCompanyId=" + targetCompanyId
                        : "redirect:/messages";
            }
            model.addAttribute("message", message);
            model.addAttribute("targetCompanyId", targetCompanyId);
            return "messages/details";
        } catch (TargetCompanyRequiredException e) {
            log.warn("Target company required for viewing message details (messageId: {}) by userId: {}", messageId, currentUser.getUserId());
            model.addAttribute("companies", companyService.getAllCompanies());
            model.addAttribute("redirectUrl", "/messages/details/" + messageId);
            return "select-company";
        } catch (Exception e) {
            log.error("Error retrieving message details for messageId: {}", messageId, e);
            return targetCompanyId != null
                    ? "redirect:/messages?targetCompanyId=" + targetCompanyId
                    : "redirect:/messages";
        }
    }

    private void populateSendForm(Model model, User currentUser, Integer targetCompanyId, Integer selectedTemplateId, MessageReqDto existingDto) {
        log.info("Populating send form data for userId: {}, targetCompanyId: {}", currentUser.getUserId(), targetCompanyId);
        List<ContactResDto> contacts = contactService.getAllContacts(currentUser.getUserId(), targetCompanyId);
        List<TemplateResDto> templates = templateService.getAllTemplatesByCompany(currentUser.getUserId(), targetCompanyId);

        MessageReqDto dto = (existingDto != null) ? existingDto : new MessageReqDto();

        if (existingDto == null && selectedTemplateId != null) {
            templates.stream()
                    .filter(t -> selectedTemplateId.equals(t.getTemplateId()))
                    .findFirst()
                    .ifPresent(t -> {
                        dto.setTemplateId(t.getTemplateId());
                        dto.setBody(t.getBody());
                    });
        }

        model.addAttribute("messageReqDto", dto);
        model.addAttribute("contacts", contacts);
        model.addAttribute("templates", templates);
        model.addAttribute("targetCompanyId", targetCompanyId);
    }
}