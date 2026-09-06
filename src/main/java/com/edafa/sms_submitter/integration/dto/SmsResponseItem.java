package com.edafa.sms_submitter.integration.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SmsResponseItem {

    @JsonProperty("ReceiverMSISDN")
    public String receiverMsisdn;

    @JsonProperty("smsId")
    public String smsId;

    @JsonProperty("SMSStatus")
    public String smsStatus;
}