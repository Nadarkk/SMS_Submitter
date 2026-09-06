package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.dto.SmsDetailResDto;
import com.edafa.sms_submitter.entity.*;
import com.edafa.sms_submitter.mapper.SmsMapper;
import com.edafa.sms_submitter.repository.SmsRepo;
import com.edafa.sms_submitter.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j

@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsRepo smsRepo;
    private final SmsMapper smsMapper;
    private final UserRepo userRepo;
    private final AccessService accessControlService;

    // Internal helper called by MessageService[cite: 13]
    @Transactional
    public Sms createSmsRecord(Message message, Contact contact, SmsStatus status) {
        log.info("Creating SMS record for messageId: {}, contactId: {}, status: {}",
                message.getMessageId(), contact.getContactId(), status);
        Sms savedSms = smsRepo.save(new Sms(status, message, contact));
        log.info("SMS record created with smsId: {}", savedSms.getSmsId());
        return savedSms;
    }

    @Transactional(readOnly = true)
    public SmsDetailResDto getSmsById(Integer smsId, Integer userId) {
        log.info("Fetching SMS log details for smsId: {}, requested by userId: {}", smsId, userId);
        User requester = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found during SMS log lookup", userId);
                    return new ResourceNotFoundException("User not found");
                });
        Sms sms = smsRepo.findById(smsId)
                .orElseThrow(() -> {
                    log.error("SMS log with ID {} not found", smsId);
                    return new ResourceNotFoundException("SMS log not found");
                });

        accessControlService.checkAccess(requester, sms.getMessage().getUser().getCompany().getCompanyId());
        return smsMapper.toDto(sms);
    }
}