package com.edafa.sms_submitter.dto;

import java.time.LocalDate;

public record SmsStatsRowDto(
        LocalDate date,
        long totalMessages,
        long totalSegments,
        long delivered,
        long failed,
        double deliveryRatePercent
) {}