package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.dto.SmsStatsRowDto;
import com.edafa.sms_submitter.entity.SmsStatus;
import com.edafa.sms_submitter.entity.User;
import com.edafa.sms_submitter.exception.ResourceNotFoundException;
import com.edafa.sms_submitter.repository.SmsRepo;
import com.edafa.sms_submitter.service.AccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SmsRepo smsRepository;
    private final AccessService accessService;

    public List<SmsStatsRowDto> getSmsStats(Integer targetCompanyId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = to.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

        List<Object[]> raw = smsRepository.findDailyStatusCounts(targetCompanyId, start, end);


        Map<LocalDate, Map<SmsStatus, Long>> byDay = new TreeMap<>();
        for (Object[] row : raw) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            SmsStatus status = (SmsStatus) row[1];
            long count = (Long) row[2];
            byDay.computeIfAbsent(day, d -> new EnumMap<>(SmsStatus.class)).put(status, count);
        }

        List<SmsStatsRowDto> result = new ArrayList<>();
        for (var entry : byDay.entrySet()) {
            Map<SmsStatus, Long> counts = entry.getValue();
            long delivered = counts.getOrDefault(SmsStatus.DELIVERED, 0L);
            long failed = counts.getOrDefault(SmsStatus.FAILED, 0L);
            long pending = counts.getOrDefault(SmsStatus.PENDING, 0L)
                    + counts.getOrDefault(SmsStatus.SUBMITTED, 0L);
            long total = delivered + failed + pending;
            double rate = total == 0 ? 0.0 : (delivered * 100.0 / total);

            result.add(new SmsStatsRowDto(entry.getKey(), total, delivered, failed, pending, rate));
        }
        return result;
    }
}