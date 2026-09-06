package com.edafa.sms_submitter.service;
import com.edafa.sms_submitter.entity.Role;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.TargetCompanyRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j

public class AccessService {

    // Decides access for each company: admin can enter any company with id, but managers can only access their own company[cite: 9]
    //For admins: return targetCompanyId[cite: 9]
    //Users/Managers: return their own companyId[cite: 9]
    public Integer resolveCompanyId(User requester, Integer targetCompanyId) {
        log.info("Resolving company ID for requester: {}, role: {}, targetCompanyId: {}",
                requester.getUserId(), requester.getRole(), targetCompanyId);
        if (requester.getRole() == Role.ADMIN) {
            if (targetCompanyId == null) {
                log.warn("Target company resolution failed for ADMIN (targetCompanyId is null)");
                throw new TargetCompanyRequiredException("Admin must explicitly select a target company.");
            }
            return targetCompanyId;
        }
        return requester.getCompany().getCompanyId();
    }

    //Check if user company equals target company or an admin is accessing[cite: 9]
    public void checkAccess(User user, Integer companyId) {
        log.info("Checking access for userId: {}, role: {}, target companyId: {}",
                user.getUserId(), user.getRole(), companyId);
        if (user.getRole() == Role.ADMIN) return;
        if (!user.getCompany().getCompanyId().equals(companyId)) {
            log.warn("Access denied for userId: {} attempting to access companyId: {}",
                    user.getUserId(), companyId);
            throw new IllegalArgumentException("Unauthorized: User does not have access to this company data");
        }
    }

}