package com.edafa.sms_submitter.integration.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// --- Submission DTOs ---
public class SmsDetails {
    @JsonProperty("SenderName")
    public String senderName;
    @JsonProperty("ReceiverMSISDN")
    public String receiverMsisdn;
    @JsonProperty("SMSText")
    public String smsText;
}