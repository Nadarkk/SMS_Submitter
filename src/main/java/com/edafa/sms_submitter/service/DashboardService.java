package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.dto.DashboardStatsDto;
import com.edafa.sms_submitter.dto.MessageResDto;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.entity.SmsStatus;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SmsRepo smsRepo;
    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final AccessService accessService;
    private final MessageService messageService;
    private final ContactRepo contactRepo;
    private final TemplateRepo templateRepo;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(Integer userId, Integer targetCompanyId) {
        User requester = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isAdmin = requester.getRole() == Role.ADMIN;

        // For Admins: targetCompanyId == null gives global view across all companies
        Integer companyId = isAdmin ? targetCompanyId :
                (targetCompanyId != null ? targetCompanyId :
                        (requester.getCompany() != null ? requester.getCompany().getCompanyId() : null));

        long totalMessages = (companyId == null) ? messageRepo.count() : messageRepo.countByUser_Company_CompanyId(companyId);
        long totalSms = (companyId == null) ? smsRepo.count() : smsRepo.countByMessage_User_Company_CompanyId(companyId);
        long delivered = (companyId == null) ? smsRepo.countByStatus(SmsStatus.DELIVERED) : smsRepo.countByMessage_User_Company_CompanyIdAndStatus(companyId, SmsStatus.DELIVERED);
        long failed = (companyId == null) ? smsRepo.countByStatus(SmsStatus.FAILED) : smsRepo.countByMessage_User_Company_CompanyIdAndStatus(companyId, SmsStatus.FAILED);
        long submitted = (companyId == null) ? smsRepo.countByStatus(SmsStatus.SUBMITTED) : smsRepo.countByMessage_User_Company_CompanyIdAndStatus(companyId, SmsStatus.SUBMITTED);
        long pending = (companyId == null) ? smsRepo.countByStatus(SmsStatus.PENDING) : smsRepo.countByMessage_User_Company_CompanyIdAndStatus(companyId, SmsStatus.PENDING);
        long totalContacts = (companyId == null) ? contactRepo.count() : contactRepo.countByCompany_CompanyId(companyId);
        long totalTemplates = (companyId == null) ? templateRepo.count() : templateRepo.countByCompany_CompanyId(companyId);

        double deliveryRate = totalSms > 0 ? ((double) delivered / totalSms) * 100.0 : 0.0;

        // MessageService already handles global vs company-scoped queries cleanly
        List<MessageResDto> recent = messageService.getAllMessagesByCompany(userId, targetCompanyId)
                .stream()
                .limit(5)
                .toList();

        return DashboardStatsDto.builder()
                .totalMessages(totalMessages)
                .totalSms(totalSms)
                .totalDelivered(delivered)
                .totalFailed(failed)
                .totalSubmitted(submitted)
                .totalPending(pending)
                .deliveryRate(Math.round(deliveryRate * 10.0) / 10.0)
                .chartDeliveredCount(delivered)
                .chartFailedCount(failed)
                .totalContacts(totalContacts)
                .totalTemplates(totalTemplates)
                .recentMessages(recent)
                .build();
    }
}