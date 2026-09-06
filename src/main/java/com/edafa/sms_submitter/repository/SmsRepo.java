package com.edafa.sms_submitter.repository;
import com.edafa.sms_submitter.entity.Sms;
import com.edafa.sms_submitter.entity.SmsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository

public interface SmsRepo extends JpaRepository<Sms, Integer> {
    SmsStatus findStatusBySmsId(Integer smsId);
    SmsStatus findStatusBySimId(String simId);
    List<Sms> findAllByMessage_MessageId(Integer messageId);
    List<Sms> findAllByStatus(SmsStatus status);
    long countByMessage_User_Company_CompanyId(Integer companyId);
    long countByMessage_User_Company_CompanyIdAndStatus(Integer companyId, SmsStatus status);
    long countByStatus(SmsStatus status);


    @Query("""
    SELECT FUNCTION('DATE', m.sendDate) as day, 
           s.status as status, 
           COUNT(s) as cnt,
           SUM(m.segmentCount) as totalSegments
    FROM Sms s
    JOIN s.message m
    JOIN m.user u
    WHERE u.company.companyId = :companyId
      AND m.sendDate BETWEEN :from AND :to
            GROUP BY FUNCTION('DATE', m.sendDate), s.status
            ORDER BY FUNCTION('DATE', m.sendDate)
    """)
    List<Object[]> findDailyStatusCounts(
            @Param("companyId") Integer companyId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
