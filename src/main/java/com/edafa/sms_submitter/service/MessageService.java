package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.dto.MessageReqDto;
import com.edafa.sms_submitter.dto.MessageResDto;
import com.edafa.sms_submitter.dto.SmsResDto;
import com.edafa.sms_submitter.entity.*;
import com.edafa.sms_submitter.mapper.MessageMapper;
import com.edafa.sms_submitter.mapper.SmsMapper;
import com.edafa.sms_submitter.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edafa.sms_submitter.integration.Web2SmsIntegrationService;
import com.edafa.sms_submitter.integration.dto.SmsDetails;
import com.edafa.sms_submitter.integration.dto.SubmitSmsResponse;
import com.edafa.sms_submitter.integration.dto.SmsResponseItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    // Simulator caps a single submitAndgetSmsId request to 1000 SMS entries.
    private static final int SUBMIT_BATCH_SIZE = 1000;

    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final ContactRepo contactRepo;
    private final TemplateRepo templateRepo;
    private final SmsRepo smsRepo;
    private final SmsService smsService;
    private final AccessService accessService;
    private final MessageMapper messageMapper;
    private final SmsMapper smsMapper;
    private final Web2SmsIntegrationService web2SmsIntegrationService;

    @Transactional
    public MessageResDto sendMessage(MessageReqDto dto, Integer userId, Integer targetCompanyId) {
        log.info("Processing sendMessage execution for userId: {}, targetCompanyId: {}, contactsCount: {}",
                userId, targetCompanyId, dto.getContactIds() != null ? dto.getContactIds().size() : 0);
        User requester = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User missing during message send attempt, userId: {}", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });
        Integer companyId = accessService.resolveCompanyId(requester, targetCompanyId);
        Template template = null;

        if (dto.getTemplateId() != null) {
            template = templateRepo.findById(dto.getTemplateId())
                    .orElseThrow(() -> {
                        log.error("Template missing during message send, templateId: {}", dto.getTemplateId());
                        return new ResourceNotFoundException("Template with ID " + dto.getTemplateId() + " not found");
                    });

            accessService.checkAccess(requester, template.getCompany().getCompanyId());
        }

        Instant currentSendDate = Instant.now();
        Message message = messageMapper.toEntity(dto, requester, template, currentSendDate);

        int segments = calculateSegmentCount(dto.getBody());
        message.setSegmentCount(segments);
        Message savedMessage = messageRepo.save(message);
        log.info("Persisted parent Message entity with messageId: {}, segmentCount: {}", savedMessage.getMessageId(), segments);

        List<Sms> savedSmsList = new ArrayList<>();
        List<SmsDetails> smsDetailsList = new ArrayList<>();
        List<SmsResDto> smsResponses = new ArrayList<>();
        for (Integer contactId : dto.getContactIds()) {
            Contact contact = contactRepo.findById(contactId)
                    .orElseThrow(() -> {
                        log.error("Contact missing during SMS creation, contactId: {}", contactId);
                        return new ResourceNotFoundException("Contact with ID " + contactId + " not found");
                    });
            accessService.checkAccess(requester, contact.getCompany().getCompanyId());

            //create sms record for each contact
            Sms savedSms = smsService.createSmsRecord(savedMessage, contact, SmsStatus.PENDING);
            savedSmsList.add(savedSms);

            // Create Web2SMS DTO
            SmsDetails smsDetails = new SmsDetails();
            smsDetails.senderName = savedMessage.getSender();
            smsDetails.receiverMsisdn = contact.getPhoneNo();
            smsDetails.smsText = savedMessage.getBody();
            smsDetailsList.add(smsDetails);
            smsResponses.add(smsMapper.toDto(savedSms, contact));
        }

        int total = savedSmsList.size();
        for (int start = 0; start < total; start += SUBMIT_BATCH_SIZE) {
            int end = Math.min(start + SUBMIT_BATCH_SIZE, total);
            List<Sms> smsChunk = savedSmsList.subList(start, end);
            List<SmsDetails> detailsChunk = smsDetailsList.subList(start, end);

            submitBatch(savedMessage.getMessageId(), smsChunk, detailsChunk);
        }

        log.info("Created {} child Sms records for messageId: {}", smsResponses.size(), savedMessage.getMessageId());
        return messageMapper.toDto(savedMessage, smsResponses);
    }

    private void submitBatch(Integer messageId, List<Sms> smsChunk, List<SmsDetails> detailsChunk) {
        SubmitSmsResponse response = null;
        try {
            response = web2SmsIntegrationService.sendSmsBatch(detailsChunk);
        } catch (Exception e) {
            log.error("Simulator submission failed for messageId {}: {}", messageId, e.getMessage(), e);
        }

        // Save returned Web2SMS smsId into simId
        if (response != null && response.smsResponseList != null) {
            for (Sms savedSms : smsChunk) {
                String contactPhone = savedSms.getContact().getPhoneNo();
                SmsResponseItem matchedItem = null;
                for (SmsResponseItem responseItem : response.smsResponseList) {
                    if (contactPhone.equals(responseItem.receiverMsisdn)) {
                        matchedItem = responseItem;
                        break;
                    }
                }
                // PENDING → SUBMITTED → DELIVERED/FAILED
                if (matchedItem == null) {
                    log.warn("No simulator response matched contactId {} (phone {}) for messageId {}; marking FAILED",
                            savedSms.getContact().getContactId(), contactPhone, messageId);
                    savedSms.setStatus(SmsStatus.FAILED);
                } else if ("SUBMITTED".equals(matchedItem.smsStatus)) {
                    savedSms.setSimId(matchedItem.smsId);
                    savedSms.setStatus(SmsStatus.SUBMITTED);
                } else {
                    savedSms.setSimId(matchedItem.smsId);
                    savedSms.setStatus(SmsStatus.FAILED);
                }
            }
        } else {
            log.warn("No usable simulator response for messageId {}; marking {} Sms rows FAILED",
                    messageId, smsChunk.size());
            for (Sms savedSms : smsChunk) {
                savedSms.setStatus(SmsStatus.FAILED);
            }
        }
    }

    @Transactional(readOnly = true)
    public MessageResDto getMessageById(Integer messageId, Integer userId) {
        log.info("Fetching message details for messageId: {}, requested by userId: {}", messageId, userId);
        User requester = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User missing during message fetch, userId: {}", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });

        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> {
                    log.error("Message record missing, messageId: {}", messageId);
                    return new ResourceNotFoundException("Message with ID " + messageId + " not found");
                });
        accessService.checkAccess(requester, message.getUser().getCompany().getCompanyId());

        List<Sms> smsList = smsRepo.findAllByMessage_MessageId(messageId);
        List<SmsResDto> smsResDtoList = smsList.stream()
                .map(sms -> smsMapper.toDto(sms, sms.getContact()))
                .toList();
        return messageMapper.toDto(message, smsResDtoList);
    }

    @Transactional(readOnly = true)
    public List<MessageResDto> getAllMessagesByCompany(Integer userId, Integer targetCompanyId) {
        log.info("Fetching all messages for userId: {}, targetCompanyId: {}", userId, targetCompanyId);
        User requester = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User missing during message history lookup, userId: {}", userId);
                    return new ResourceNotFoundException("User with ID " + userId + " not found");
                });

        boolean isAdmin = requester.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Message> messages;

        // Default global view for admins when targetCompanyId is null
        if (isAdmin && targetCompanyId == null) {
            log.info("Admin fetching messages globally across all companies");
            messages = messageRepo.findAllByOrderBySendDateDesc();
        } else {
            Integer companyId = accessService.resolveCompanyId(requester, targetCompanyId);
            messages = messageRepo.findAllByCompanyIdOrderBySendDateDesc(companyId);
            log.info("Retrieved {} message records for companyId: {}", messages.size(), companyId);
        }

        return messages.stream()
                .map(message -> {
                    List<Sms> smsList = smsRepo.findAllByMessage_MessageId(message.getMessageId());
                    List<SmsResDto> smsResDtoList = smsList.stream()
                            .map(sms -> smsMapper.toDto(sms, sms.getContact()))
                            .toList();
                    return messageMapper.toDto(message, smsResDtoList);
                })
                .toList();
    }


    //helper fn
    private int calculateSegmentCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Check if string contains characters outside standard GSM-7 basic charset
        boolean isGsm = text.matches("^[A-Za-z0-9 \\r\\n@£$¥èéùìòÇØøÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ!\"#$%&'()*+,-./:;<=>?¡ÄÖÑÜ§àäöñüà]*$");
        int length = text.length();
        if (isGsm) {
            if (length <= 160) return 1;
            return (int) Math.ceil((double) length / 153.0);
        } else {
            // UCS-2 encoding (Arabic, Emojis, etc.)
            if (length <= 70) return 1;
            return (int) Math.ceil((double) length / 67.0);
        }
    }
}