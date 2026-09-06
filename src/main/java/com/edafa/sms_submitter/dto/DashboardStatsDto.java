package com.edafa.sms_submitter.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DashboardStatsDto {
    private long totalMessages;
    private long totalSms;
    private long totalDelivered;
    private long totalFailed;
    private long totalSubmitted;
    private long totalPending;
    private double deliveryRate;

    // Data for Chart.js
    private long chartDeliveredCount;
    private long chartFailedCount;
    private long totalContacts;
    private long totalTemplates;
    // Recent Messages Table
    private List<MessageResDto> recentMessages;
}