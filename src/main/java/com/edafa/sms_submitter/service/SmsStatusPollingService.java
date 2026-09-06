package com.edafa.sms_submitter.service;

import com.edafa.sms_submitter.entity.Sms;
import com.edafa.sms_submitter.entity.SmsStatus;
import com.edafa.sms_submitter.integration.Web2SmsIntegrationService;
import com.edafa.sms_submitter.integration.dto.InquireSmsResponse;
import com.edafa.sms_submitter.integration.dto.InquiryResponseItem;
import com.edafa.sms_submitter.repository.SmsRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsStatusPollingService {

    private final SmsRepo smsRepo;
    private final Web2SmsIntegrationService web2SmsIntegrationService;
    // Simulator caps a single inquireBySmsId request to 250 smsIds.
    private static final int INQUIRE_BATCH_SIZE = 250;

    @Scheduled(fixedDelayString = "${web2sms.polling.fixed-delay}")
    @Transactional
    public void pollPendingSmsStatuses() {
        log.debug("Polling background thread started...");
        // 1. Fetch the tracking IDs of messages that are currently marked as "PENDING" in your database
        List<Sms> submittedSms = smsRepo.findAllByStatus(SmsStatus.SUBMITTED).stream()
                .filter(sms -> sms.getSimId() != null) //extra check
                .toList();

        if (submittedSms.isEmpty()) {
            return;
        }
        log.info("Found {} SUBMITTED SMS to poll.", submittedSms.size());

        List<List<Sms>> smsBatches = partition(submittedSms, INQUIRE_BATCH_SIZE);

        for (List<Sms> smsBatch : smsBatches) {
            pollBatch(smsBatch);
        }
    }

    private void pollBatch(List<Sms> smsBatch) {
        List<String> simIds = smsBatch.stream().map(Sms::getSimId).toList();

        InquireSmsResponse response;
        try {
            // 2. Call the integration service to get the latest statuses
            response = web2SmsIntegrationService.inquireStatus(simIds);
        } catch (Exception e) {
            log.error("Failed to poll message statuses from the simulator: {}", e.getMessage(), e);
            return;
        }

        if (response == null || !"SUCCESS".equals(response.resultStatus) || response.responseList == null) {
            log.warn("Simulator inquiry returned an unsuccessful/empty response: {}",
                    response != null ? response.description : "null");
            return;
        }

        // index the response by simId so we can look each one up
        // while iterating our own rows, instead of nested looping.
        Map<String, String> simIdToSimulatorStatus = response.responseList.stream()
                .collect(Collectors.toMap(
                        item -> item.smsId,
                        item -> item.smsStatus,
                        (existing, duplicate) -> existing));

        for (Sms sms : smsBatch) {
            String simulatorStatus = simIdToSimulatorStatus.get(sms.getSimId());
            if (simulatorStatus == null) {
                // Simulator didn't return this one this round; leave as-is, try again next poll.
                continue;
            }

            SmsStatus mapped = switch (simulatorStatus) {
                case "DELIVERED" -> SmsStatus.DELIVERED;
                case "NOT_DELIVERED" -> SmsStatus.FAILED;
                default -> {
                    log.warn("Unknown simulator status '{}' for simId {}, ignoring", simulatorStatus, sms.getSimId());
                    yield null;
                }
            };

            if (mapped != null) {
                log.info("SMS ID: {} (simId: {}) transitioning {} -> {}",
                        sms.getSmsId(), sms.getSimId(), sms.getStatus(), mapped);
                sms.setStatus(mapped);
            }
        }
    }

    private <T> List<List<T>> partition(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }
}